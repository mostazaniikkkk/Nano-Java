/*
 * classfile.c - class file parser. Produces an unlinked Class (state
 * CLS_LOADED); class.c resolves the hierarchy and lays out the fields.
 */
#include "vm.h"

#include <stdlib.h>

typedef struct {
    const uint8_t *p, *end;
    bool           bad;
} Reader;

static uint8_t u1(Reader *r)
{
    if (r->p + 1 > r->end) {
        r->bad = true;
        return 0;
    }
    return *r->p++;
}

static uint16_t u2(Reader *r)
{
    if (r->p + 2 > r->end) {
        r->bad = true;
        r->p = r->end;
        return 0;
    }
    uint16_t v = (uint16_t)((r->p[0] << 8) | r->p[1]);
    r->p += 2;
    return v;
}

static uint32_t u4(Reader *r)
{
    if (r->p + 4 > r->end) {
        r->bad = true;
        r->p = r->end;
        return 0;
    }
    uint32_t v = ((uint32_t)r->p[0] << 24) | (r->p[1] << 16) | (r->p[2] << 8) | r->p[3];
    r->p += 4;
    return v;
}

static void skip(Reader *r, uint32_t n)
{
    if ((uint32_t)(r->end - r->p) < n) {
        r->bad = true;
        r->p = r->end;
    } else {
        r->p += n;
    }
}

static const char *cp_utf8(Class *c, uint16_t idx)
{
    if (idx == 0 || idx >= c->cp_count || c->cp[idx].tag != CONSTANT_Utf8)
        return NULL;
    return c->cp[idx].v.utf8;
}

static const char *cp_class_name(Class *c, uint16_t idx)
{
    if (idx == 0 || idx >= c->cp_count || c->cp[idx].tag != CONSTANT_Class)
        return NULL;
    return cp_utf8(c, c->cp[idx].v.ref.a);
}

/* Number of argument slots described by a method descriptor. */
int desc_arg_slots(const char *desc)
{
    int         n = 0;
    const char *p = desc + 1;
    while (*p && *p != ')') {
        switch (*p) {
        case 'J': case 'D':
            n += 2;
            p++;
            break;
        case 'L':
            n++;
            while (*p && *p != ';')
                p++;
            if (*p)
                p++;
            break;
        case '[':
            n++;
            while (*p == '[')
                p++;
            if (*p == 'L') {
                while (*p && *p != ';')
                    p++;
            }
            if (*p)
                p++;
            break;
        default:
            n++;
            p++;
            break;
        }
    }
    return n;
}

static bool parse_code(Reader *r, Class *c, Method *m)
{
    m->max_stack = u2(r);
    m->max_locals = u2(r);
    m->code_len = u4(r);
    if (r->bad || m->code_len == 0 || m->code_len > (uint32_t)(r->end - r->p))
        return false;
    /* A little padding lets the interpreter read operands of a truncated
     * final instruction without running off the buffer. */
    m->code = nj_meta(m->code_len + 4);
    memcpy(m->code, r->p, m->code_len);
    memset(m->code + m->code_len, 0, 4);
    r->p += m->code_len;

    m->n_exc = u2(r);
    if (m->n_exc) {
        m->exc = nj_meta(m->n_exc * sizeof(ExcEntry));
        for (int i = 0; i < m->n_exc; i++) {
            m->exc[i].start = u2(r);
            m->exc[i].end = u2(r);
            m->exc[i].handler = u2(r);
            m->exc[i].catch_type = u2(r);
        }
    }

    uint16_t nattr = u2(r);
    for (int i = 0; i < nattr && !r->bad; i++) {
        const char *name = cp_utf8(c, u2(r));
        uint32_t    len = u4(r);
        const uint8_t *next = r->p + len;
        if (name && strcmp(name, "LineNumberTable") == 0 && !m->lines) {
            m->n_lines = u2(r);
            m->lines = nj_meta(m->n_lines * sizeof(LineEntry) + 1);
            for (int j = 0; j < m->n_lines; j++) {
                m->lines[j].pc = u2(r);
                m->lines[j].line = u2(r);
            }
        }
        if (next > r->end) {
            r->bad = true;
            break;
        }
        r->p = next;
    }
    return !r->bad;
}

static void parse_return(Method *m)
{
    const char *p = strchr(m->desc, ')');
    char        rt = p ? p[1] : 'V';
    m->ret_type = rt;
    m->ret_slots = rt == 'V' ? 0 : (rt == 'J' || rt == 'D') ? 2 : 1;
}

Class *classfile_parse(const uint8_t *data, uint32_t len, const char **err)
{
    Reader r = {data, data + len, false};
    *err = NULL;

    if (u4(&r) != 0xCAFEBABE) {
        *err = "bad magic";
        return NULL;
    }
    u2(&r);                       /* minor */
    uint16_t major = u2(&r);
    if (major < 45 || major > 52) {
        *err = "unsupported class file version";
        return NULL;
    }

    Class *c = nj_meta(sizeof(Class));
    memset(c, 0, sizeof *c);
    c->cp_count = u2(&r);
    c->cp = nj_meta(sizeof(CPEntry) * (c->cp_count ? c->cp_count : 1));
    memset(c->cp, 0, sizeof(CPEntry) * (c->cp_count ? c->cp_count : 1));

    for (int i = 1; i < c->cp_count; i++) {
        CPEntry *e = &c->cp[i];
        e->tag = u1(&r);
        switch (e->tag) {
        case CONSTANT_Utf8: {
            uint16_t n = u2(&r);
            if ((uint32_t)(r.end - r.p) < n) {
                r.bad = true;
                break;
            }
            e->v.utf8 = sym_intern_len((const char *)r.p, n);
            r.p += n;
            break;
        }
        case CONSTANT_Integer:
        case CONSTANT_Float:
            e->v.u = u4(&r);
            break;
        case CONSTANT_Long:
        case CONSTANT_Double:
            /* High word in this entry, low word in the next. */
            e->v.u = u4(&r);
            if (i + 1 < c->cp_count) {
                c->cp[i + 1].tag = 0;
                c->cp[i + 1].v.u = u4(&r);
            } else {
                r.bad = true;
            }
            i++;
            break;
        case CONSTANT_Class:
        case CONSTANT_String:
            e->v.ref.a = u2(&r);
            break;
        case CONSTANT_Fieldref:
        case CONSTANT_Methodref:
        case CONSTANT_InterfaceMethodref:
        case CONSTANT_NameAndType:
            e->v.ref.a = u2(&r);
            e->v.ref.b = u2(&r);
            break;
        default:
            *err = "unsupported constant pool tag";
            return NULL;
        }
        if (r.bad)
            break;
    }
    if (r.bad) {
        *err = "truncated constant pool";
        return NULL;
    }

    c->access = u2(&r);
    c->name = cp_class_name(c, u2(&r));
    uint16_t super_idx = u2(&r);
    const char *super_name = super_idx ? cp_class_name(c, super_idx) : NULL;
    if (!c->name || (super_idx && !super_name)) {
        *err = "bad class name";
        return NULL;
    }
    if (c->access & ACC_INTERFACE)
        c->flags |= CF_INTERFACE;

    c->n_interfaces = u2(&r);
    uint16_t *iface_idx = NULL;
    if (c->n_interfaces) {
        iface_idx = malloc(c->n_interfaces * sizeof *iface_idx);
        for (int i = 0; i < c->n_interfaces; i++)
            iface_idx[i] = u2(&r);
    }

    c->n_fields = u2(&r);
    c->fields = nj_meta(sizeof(Field) * (c->n_fields ? c->n_fields : 1));
    memset(c->fields, 0, sizeof(Field) * (c->n_fields ? c->n_fields : 1));
    for (int i = 0; i < c->n_fields && !r.bad; i++) {
        Field *f = &c->fields[i];
        f->cls = c;
        f->access = u2(&r);
        f->name = cp_utf8(c, u2(&r));
        f->desc = cp_utf8(c, u2(&r));
        uint16_t nattr = u2(&r);
        for (int j = 0; j < nattr && !r.bad; j++) {
            const char *an = cp_utf8(c, u2(&r));
            uint32_t    alen = u4(&r);
            if (an && strcmp(an, "ConstantValue") == 0 && alen == 2)
                f->const_index = u2(&r);
            else
                skip(&r, alen);
        }
        if (!f->name || !f->desc)
            r.bad = true;
    }

    c->n_methods = u2(&r);
    c->methods = nj_meta(sizeof(Method) * (c->n_methods ? c->n_methods : 1));
    memset(c->methods, 0, sizeof(Method) * (c->n_methods ? c->n_methods : 1));
    for (int i = 0; i < c->n_methods && !r.bad; i++) {
        Method *m = &c->methods[i];
        m->cls = c;
        m->access = u2(&r);
        m->name = cp_utf8(c, u2(&r));
        m->desc = cp_utf8(c, u2(&r));
        if (!m->name || !m->desc || m->desc[0] != '(') {
            r.bad = true;
            break;
        }
        m->arg_slots = (uint16_t)(desc_arg_slots(m->desc) + ((m->access & ACC_STATIC) ? 0 : 1));
        parse_return(m);
        uint16_t nattr = u2(&r);
        for (int j = 0; j < nattr && !r.bad; j++) {
            const char *an = cp_utf8(c, u2(&r));
            uint32_t    alen = u4(&r);
            const uint8_t *next = r.p + alen;
            if (next > r.end) {
                r.bad = true;
                break;
            }
            if (an && strcmp(an, "Code") == 0 && !m->code) {
                Reader sub = {r.p, next, false};
                if (!parse_code(&sub, c, m))
                    r.bad = true;
            }
            r.p = next;
        }
    }

    uint16_t nattr = u2(&r);
    for (int i = 0; i < nattr && !r.bad; i++) {
        const char *an = cp_utf8(c, u2(&r));
        uint32_t    alen = u4(&r);
        if (an && strcmp(an, "SourceFile") == 0 && alen == 2)
            c->source_file = cp_utf8(c, u2(&r));
        else
            skip(&r, alen);
    }

    if (r.bad) {
        free(iface_idx);
        *err = "truncated class file";
        return NULL;
    }

    /* The superclass and interface names reach the linker through
     * c->interfaces: names[0] is the superclass (NULL for Object), followed
     * by the interfaces. class.c replaces this with resolved classes. */
    const char **names = nj_meta(sizeof(char *) * (c->n_interfaces + 1));
    names[0] = super_name;
    for (int i = 0; i < c->n_interfaces; i++) {
        names[i + 1] = cp_class_name(c, iface_idx[i]);
        if (!names[i + 1]) {
            free(iface_idx);
            *err = "bad interface name";
            return NULL;
        }
    }
    free(iface_idx);
    c->interfaces = (Class **)names;  /* replaced by class.c when linking */
    c->state = CLS_LOADED;
    return c;
}
