/*
 * string.c - java.lang.String helpers and the intern table.
 *
 * A String is { char[] value; int offset; int count; } (see
 * classlib/src/java/lang/String.java).
 */
#include "vm.h"

#include <stdlib.h>

static Object **intern_tab;
static uint32_t intern_cap, intern_count;

jint str_length(Object *s)
{
    return GET_INT(s, vm.f_String_count);
}

const jchar *str_chars(Object *s)
{
    Object *value = GET_REF(s, vm.f_String_value);
    return ARRAY_DATA(value, jchar) + GET_INT(s, vm.f_String_offset);
}

Object *str_new_chars(Thread *t, const jchar *chars, jint len)
{
    Array *value = vm_new_prim_array(t, 'C', len);
    if (!value)
        return NULL;
    if (len)
        memcpy(ARRAY_DATA(value, jchar), chars, (size_t)len * sizeof(jchar));
    Object *s = vm_new_object(t, vm.c_String);
    if (!s)
        return NULL;
    SET_REF(s, vm.f_String_value, (Object *)value);
    SET_INT(s, vm.f_String_offset, 0);
    SET_INT(s, vm.f_String_count, len);
    return s;
}

/* Decodes (modified) UTF-8. Malformed bytes are passed through as Latin-1,
 * which is what most J2ME devices did with badly encoded resources. */
static jint utf8_decode(const uint8_t *s, size_t len, jchar *out)
{
    jint   n = 0;
    size_t i = 0;
    while (i < len) {
        uint8_t c = s[i];
        if (c < 0x80) {
            if (out) out[n] = c;
            i++;
        } else if ((c & 0xE0) == 0xC0 && i + 1 < len && (s[i + 1] & 0xC0) == 0x80) {
            if (out) out[n] = (jchar)(((c & 0x1F) << 6) | (s[i + 1] & 0x3F));
            i += 2;
        } else if ((c & 0xF0) == 0xE0 && i + 2 < len && (s[i + 1] & 0xC0) == 0x80 &&
                   (s[i + 2] & 0xC0) == 0x80) {
            if (out) out[n] = (jchar)(((c & 0x0F) << 12) | ((s[i + 1] & 0x3F) << 6) | (s[i + 2] & 0x3F));
            i += 3;
        } else {
            if (out) out[n] = c;
            i++;
        }
        n++;
    }
    return n;
}

Object *str_new_utf8_len(Thread *t, const char *s, size_t len)
{
    jint   n = utf8_decode((const uint8_t *)s, len, NULL);
    Array *value = vm_new_prim_array(t, 'C', n);
    if (!value)
        return NULL;
    utf8_decode((const uint8_t *)s, len, ARRAY_DATA(value, jchar));
    Object *str = vm_new_object(t, vm.c_String);
    if (!str)
        return NULL;
    SET_REF(str, vm.f_String_value, (Object *)value);
    SET_INT(str, vm.f_String_offset, 0);
    SET_INT(str, vm.f_String_count, n);
    return str;
}

Object *str_new_utf8(Thread *t, const char *s)
{
    return str_new_utf8_len(t, s, strlen(s));
}

/* Encodes to modified UTF-8. Returns the full encoded length; writes as
 * many whole characters as fit in out (always NUL terminated if size > 0). */
static size_t utf8_encode(const jchar *c, jint n, char *out, size_t size)
{
    size_t total = 0, written = 0;
    bool   full = !out || size == 0;
    for (jint i = 0; i < n; i++) {
        jchar ch = c[i];
        char  tmp[3];
        int   need;
        if (ch != 0 && ch < 0x80) {
            tmp[0] = (char)ch;
            need = 1;
        } else if (ch < 0x800) {
            tmp[0] = (char)(0xC0 | (ch >> 6));
            tmp[1] = (char)(0x80 | (ch & 0x3F));
            need = 2;
        } else {
            tmp[0] = (char)(0xE0 | (ch >> 12));
            tmp[1] = (char)(0x80 | ((ch >> 6) & 0x3F));
            tmp[2] = (char)(0x80 | (ch & 0x3F));
            need = 3;
        }
        if (!full) {
            if (written + need < size) {
                memcpy(out + written, tmp, need);
                written += need;
            } else {
                full = true;
            }
        }
        total += need;
    }
    if (out && size)
        out[written] = 0;
    return total;
}

char *str_to_utf8(Object *s, char *buf, size_t size)
{
    if (!s) {
        if (size) buf[0] = 0;
        return buf;
    }
    utf8_encode(str_chars(s), str_length(s), buf, size);
    return buf;
}

char *str_dup_utf8(Object *s)
{
    size_t n = utf8_encode(str_chars(s), str_length(s), NULL, 0);
    char  *buf = nj_alloc(n + 1);
    utf8_encode(str_chars(s), str_length(s), buf, n + 1);
    return buf;
}

/* ------------------------------------------------------------------------ */
/* Intern table                                                             */

static uint32_t chars_hash(const jchar *c, jint n)
{
    uint32_t h = 0;
    for (jint i = 0; i < n; i++)
        h = 31 * h + c[i];
    return h;
}

static bool str_equals(Object *s, const jchar *c, jint n)
{
    return str_length(s) == n && memcmp(str_chars(s), c, (size_t)n * sizeof(jchar)) == 0;
}

static void intern_grow(void)
{
    uint32_t ncap = intern_cap ? intern_cap * 2 : 512;
    Object **nt = nj_alloc(sizeof(Object *) * ncap);
    for (uint32_t i = 0; i < intern_cap; i++) {
        Object *s = intern_tab[i];
        if (!s)
            continue;
        uint32_t h = chars_hash(str_chars(s), str_length(s)) & (ncap - 1);
        while (nt[h])
            h = (h + 1) & (ncap - 1);
        nt[h] = s;
    }
    free(intern_tab);
    intern_tab = nt;
    intern_cap = ncap;
}

static Object **intern_lookup(const jchar *c, jint n)
{
    if (intern_count * 2 >= intern_cap)
        intern_grow();
    uint32_t h = chars_hash(c, n) & (intern_cap - 1);
    while (intern_tab[h] && !str_equals(intern_tab[h], c, n))
        h = (h + 1) & (intern_cap - 1);
    return &intern_tab[h];
}

Object *str_intern(Thread *t, Object *s)
{
    (void)t;
    Object **slot = intern_lookup(str_chars(s), str_length(s));
    if (!*slot) {
        *slot = s;
        intern_count++;
    }
    return *slot;
}

Object *str_literal(Thread *t, const char *utf8)
{
    Object *s = str_new_utf8(t, utf8);
    return s ? str_intern(t, s) : NULL;
}

void str_reset(void)
{
    free(intern_tab);
    intern_tab = NULL;
    intern_cap = intern_count = 0;
}

void str_mark_roots(void (*mark)(Object *))
{
    for (uint32_t i = 0; i < intern_cap; i++)
        if (intern_tab[i])
            mark(intern_tab[i]);
}
