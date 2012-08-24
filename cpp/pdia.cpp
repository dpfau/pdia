#include <iostream>
#include <vector>
#include <string>
using namespace std;

int discreteSample(double * cdf, int size) {
	double samp = cdf[size-1] * rand();
	for (int i = 0; i < size; i++) {
		if (cdf[i] > samp) {
			return i;
		}
	}
}

template <class T>
class Datum {
	public:
		T * value;
		Datum() { value = NULL; }
};

template <class T>
class Distribution {
	public:
		void sample(Datum<T> * d);
		double score(T t);
};

template <class T>
class Constant: public Distribution<T> {
	T value;
	public:
		Constant(T t) { value = t; }
		void sample(Datum<T> * d) { d->value = &value; }
		double score(T t) { return t == value ? 1:0; }
		
};

class Random: public Distribution<Constant<float>* > {
	public:
		void sample(Datum<Constant<float> > * d) { d->value = new Constant<float>(rand()); }
		double score(Constant<float>* t) { return 1; }	
};

template<class T>
class Table;

template<class T>
class Customer: public Datum<T> {
	Table<T> * table;
	public:
		Customer() { table = NULL; }
};

template <class T>
class Table: public Distribution<T>, Datum<Distribution<T> > {
	Distribution<T> * value;
	vector<Customer<T> > customers;
	public:
		Table(Distribution<T> * t) {
			customers = new vector<Customer<T> >(); 
		}
		void sample(Datum<T> * d) { 
			if (value != NULL) {
				value->sample(d);
			}
		}
		double score(T t) { return value->score(t); }
		void add(Customer<T> c) {
			customers.add(c);
		}
};

template <class T>
class Restaurant: public Distribution<T> {
	vector<Table<T> > tables;
	Distribution<Distribution<T>* > * base; // e.g. a Random base distribution is a distribution over Constants, which are a distribution over floats
	double concentration;
	float discount;
	int sampleTable();
	public:
		T sample(); // Sample without changing state
		void sample(Customer<T> * c); // This changes the state of the Restaurant object.  Can avoid change of state by immediately removing the result, and later down the line it would be faster to have a sample-no-add method.
		double score(T t);
};

template <class T>
int Restaurant<T>::sampleTable() {
	double cdf [tables.size() + 1];
	cdf[0] = concentration + tables.size() * discount;
	for (int i = 0; i < tables.size(); i++) {
		cdf[i+1] = cdf[i] + tables[i].size() - discount;
	}
	return discreteSample(&cdf, tables.size() + 1);
}


template <class T>
void Restaurant<T>::sample(Customer<T> * c) {
	int samp = sampleTable();
	Table<T> table;
	if (samp == 0) {
		table = new Table<T>();
		base->sample(table);
		tables.add(table);
	} else {
		table = tables[samp-1];
	}
	T t = table.sample();
	table.add(*c);
	return t;
}

int main() {
	return 1;
}