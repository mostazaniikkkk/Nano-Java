//Torlus - get rid of -lm.

double
fmod(double x, double y)
{
	long	i;
	double val;
	double frac;

	if (y == 0) {
		return 0;
	}
	val = x / y;
    	i = val;
	return x - i * y;
}
