#include <iostream>
#include <vector>
using namespace std;

int discreteSample(double * cdf, int size) {
	double samp = cdf[size-1] * (double)rand()/(double)RAND_MAX;
	for (int i = 0; i < size; i++) {
		if (cdf[i] > samp) {
			return i;
		}
	}
}

template <class T>
class Value {
	public:
		virtual T value() = 0;
};

template <class T>
class BaseValue: public Value<T> {
	T val;
	public:
		BaseValue(T t) { val = t; }
		T value() { return val; }	
};


template <class T>
class ValuePtr: public Value<T> {
	Value<T> * val;
	public:
		ValuePtr() { val = NULL; }
		ValuePtr(Value<T> * v) { set(v); }
		void set(Value<T> * v) { val = v; }
		T value() { return val->value(); }	
};

template <class T>
class Distribution {
	public:
		virtual Value<T> * sample() = 0;
		virtual double score(T t) = 0;
		virtual void sample(ValuePtr<T> * v) { v->set(sample()); }
};

class Random: public Distribution<float> {
	public:
		Value<float> * sample() { return new BaseValue<float>(rand()); }
		double score(float t) { return 1; }
};

template <class T>
class Table: public ValuePtr<T> {
	vector<ValuePtr<T> * > customers;
	public:
		Table(Value<T> * v): ValuePtr<T>(v) { customers = vector<ValuePtr<T> * >(); }
		int size() { return customers.size(); }
		void add(ValuePtr<T> * v) {
			v->set(this);
			customers.push_back(v); 
		}
};

template <class T>
class Restaurant: public Distribution<T> {
	Distribution<T> * base;
	vector<Table<T> > tables;
	double concentration;
	float discount;
	public:
		Restaurant(Distribution<T> * d) { 
			base = d; 
			concentration = 1.0;
			discount = 0.0;
		}
		Value<T> * sample();
		double score(T t) {}
		void sample(ValuePtr<T> * v);
};

template <class T>
Value<T> * Restaurant<T>::sample() {
	double * cdf = new double[tables.size() + 1];
	cdf[0] = concentration + tables.size() * discount;
	for (int i = 0; i < tables.size(); i++) {
		cdf[i+1] = cdf[i] + tables[i].size() - discount;
	}
	int i = discreteSample(cdf, tables.size() + 1);
	if (i == 0) {
		Value<T> * v = base->sample();
		Table<T> t = Table<T>(v);
		tables.push_back(t);
		i = tables.size();
	}
	return &tables[i-1];
}

template <class T>
void Restaurant<T>::sample(ValuePtr<T> * v) {
	Table<T> * t = static_cast<Table<T> * > (sample());
	t->add(v);
}

int main() {
	Random * r = new Random(); 
	r->sample();
	Restaurant<float> rest = Restaurant<float>(r);
	vector<ValuePtr<float>* > * vv = new vector<ValuePtr<float>* >();
	for (int i = 0; i < 100; i++) {
		ValuePtr<float> * v = new ValuePtr<float>();
		rest.sample(v);
	}
}