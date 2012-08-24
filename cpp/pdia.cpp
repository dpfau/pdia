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
class Distribution {
	public:
		T sample();
		double score(T t);
};

template <class T>
class Constant: public Distribution<T> {
	T value;
	public:
		Constant(T t) { value = t; }
		T sample() { return value; }
		double score(T t) { return t == value ? 1:0; }
		
};

class Random: public Distribution<Constant<float>* > {
	public:
		Constant<float>* sample() { return new Constant<float>(rand()); }
		double score(Constant<float>* t) { return 1; }	
};

template<class T>
class Table;

template<class T>
class Customer {
	T val;
	Table<T> * table;
	public:
		Customer(T t) { val = t; }	
};

template <class T>
class Table: public Distribution<T> {
	Distribution<T> * dish;
	vector<Customer<T> > customers;
	public:
		Table(Distribution<T> * t) {
			dish = t; 
			customers = new vector<Customer<T> >(); 
		}
		T sample() { return dish->sample(); }
		double score(T t) { return dish->score(t); }
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
		T sample();
		T sampleAndAdd();
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
T Restaurant<T>::sample() {
	int samp = sampleTable();
	if (samp == 0) {
		return base->sample()->sample();
	} else {
		return tables[samp-1]->sample();
	}
}

template <class T>
T Restaurant<T>::sampleAndAdd() {
	int samp = sampleTable();
	Table<T> table;
	if (samp == 0) {
		table = new Table<T>(base->sample());
		tables.add(table);
	} else {
		table = tables[samp-1];
	}
	T t = table.sample();
	table.add(new Customer<T>(t));
	return t;
}

int main() {
	return 1;
}