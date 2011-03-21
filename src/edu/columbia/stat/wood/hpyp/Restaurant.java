package edu.columbia.stat.wood.hpyp;

import edu.columbia.stat.wood.pdia.Util;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Random;
import java.util.TreeMap;

public class Restaurant extends TreeMap<Integer, Restaurant> implements Serializable {

    public MutableDouble discount;
    public MutableDouble concentration;
    public TreeMap<Integer, int[]> tableMap;
    public int customers;
    public int tables;
    public Restaurant parent;
    public static int restCount = 0;
    public static final Random RNG = new Random(0L);
    private static final long serialVersionUID = 1L;

    public Restaurant(Restaurant parent, MutableDouble discount, MutableDouble concentration) {
        this.parent = parent;
        this.discount = discount;
        this.concentration = concentration;
        tableMap = new TreeMap();
        customers = 0;
        tables = 0;
        restCount += 1;
    }

    public Restaurant() {
    }

    public Restaurant getParent() {
        return parent;
    }

    public void getCounts(int type, int[] counts) {
        int cust = 0;
        int tbls = 0;
        int[] tsa = tableMap.get(type);

        if (tsa != null) {
            for (int c : tsa) {
                cust += c;
            }
            tbls = tsa.length;
        }

        counts[0] = cust;
        counts[1] = tbls;
    }

    public int tables() {
        return tables;
    }

    public double predictiveProbability(int type) {
        double d = discount.doubleVal();
        double c = concentration.doubleVal();
        double pp = parent.predictiveProbability(type);
        int[] tCounts;
        getCounts(type, tCounts = new int[2]);
        double denom = customers + c;

        return (tCounts[0] - tCounts[1] * d + (tables * d + c) * pp) / denom;
    }

    public void predictiveProbability(double[] p) {
        parent.predictiveProbability(p);

        double d = discount.doubleVal();
        double c = concentration.doubleVal();
        int[] tCounts = new int[2];

        for (int i = 0; i < p.length; i++) {
            p[i] *= (tables * d + c) / (customers + c);
        }

        for (Integer type : tableMap.keySet()) {
            getCounts(type, tCounts);
            p[type] += (tCounts[0] - tCounts[1] * d) / (customers + c);
        }
    }

    public int generate() {
        int generatedValue = -1;
        double d = discount.doubleVal();
        double c = concentration.doubleVal();
        double r = RNG.nextDouble();
        double thisRestCutoff = (customers - tables * d) / (customers + c);
        double cuSum;
        int[] tc;
        if (r < thisRestCutoff) {
            cuSum = 0.0;
            tc = new int[2];
            for (Integer type : tableMap.keySet()) {
                getCounts(type, tc);
                cuSum += (tc[0] - tc[1] * d) / (customers + c);
                if (cuSum > r) {
                    generatedValue = type;
                    break;
                }
            }
        } else {
            generatedValue = parent.generate();
        }

        assert (generatedValue > -1);
        return generatedValue;
    }

    public int[] seat(int type) {
        int[] tsa = tableMap.get(type);
        boolean seatInParent = true;

        if (tsa == null) {
            tableMap.put(type, tsa = new int[]{1});
        } else {
            double d = discount.doubleVal();
            double c = concentration.doubleVal();
            double pp = parent.predictiveProbability(type);
            double rand = RNG.nextDouble();
            double cuSum = 0.0D;
            int[] tCounts;
            getCounts(type, tCounts = new int[2]);
            double totalWeight = tCounts[0] - tCounts[1] * d + (tables() * d + c) * pp;

            for (int i = 0; i < tsa.length; i++) {
                cuSum += (tsa[i] - d) / totalWeight;
                if (cuSum > rand) {
                    tsa[i] += 1;
                    seatInParent = false;
                    break;
                }
            }

            if (cuSum <= rand) {
                int[] nSeatingArrangement = new int[tsa.length + 1];
                System.arraycopy(tsa, 0, nSeatingArrangement, 0, tsa.length);
                nSeatingArrangement[tsa.length] = 1;
                tableMap.put(type, nSeatingArrangement);
                tsa = nSeatingArrangement;
            }
        }

        customers += 1;
        if (seatInParent) {
            tables += 1;
            parent.seat(type);
        }

        return tsa;
    }

    public void seatKN(int type) {
        int[] tsa = tableMap.get(type);
        boolean seatInParent = true;

        customers += 1;
        if (tsa == null) {
            tableMap.put(type, new int[]{1});
            tables += 1;
        } else {
            tsa[0] += 1;
            seatInParent = false;
        }

        if (seatInParent) {
            parent.seatKN(type);
        }
    }

    public void unseat(int type) {
        boolean unseatInParent = true;
        int[] tsa = tableMap.get(type);
        double random = RNG.nextDouble();
        int tc = 0;
        int ind = -1;
        double cuSum = 0.0D;

        for (int tableSize : tsa) {
            tc += tableSize;
        }

        for (int i = 0; i < tsa.length; i++) {
            cuSum += (double) tsa[i] / tc;
            if (cuSum > random) {
                tsa[i] -= 1;
                ind = i;
                break;
            }
        }

        if (ind == -1) {
            System.out.println("hm");
        }

        if (tsa[ind] == 0) {
            if (tsa.length == 1) {
                tableMap.remove(type);
            } else {
                int[] nSeatingArrangement = new int[tsa.length - 1];
                System.arraycopy(tsa, 0, nSeatingArrangement, 0, ind);
                System.arraycopy(tsa, ind + 1, nSeatingArrangement, ind, tsa.length - 1 - ind);
                tableMap.put(type, nSeatingArrangement);
            }
        } else {
            unseatInParent = false;
        }

        this.customers -= 1;
        if (unseatInParent) {
            tables -= 1;
            parent.unseat(type);
        }
    }

    public void sampleTable(int type, int table) {
        double d = discount.doubleVal();
        double c = concentration.doubleVal();
        int[] tsa = tableMap.get(type);
        int tt = 0;
        int tc = -1;

        for (int i = 0; i < tsa.length; i++) {
            tc += tsa[i];
            if (tsa[i] > 0) {
                tt++;
            }
        }

        int customersToSample = tsa[table];
        for (int iter = 0; iter < customersToSample; iter++) {
            int zeroInd = -1;

            tsa[table] -= 1;
            if (tsa[table] == 0) {
                zeroInd = table;
                parent.unseat(type);
                tables -= 1;
                tt--;
            }

            double r = RNG.nextDouble();
            double cuSum = 0.0;
            double pp = parent.predictiveProbability(type);
            double totalWeight = tc - tt * d + (tables * d + c) * pp;

            for (int i = 0; i < tsa.length; i++) {
                if (tsa[i] > 0) {
                    cuSum += (tsa[i] - d) / totalWeight;
                } else {
                    zeroInd = i;
                }
                if (cuSum > r) {
                    tsa[i] += 1;
                    break;
                }
            }

            if (cuSum <= r) {
                tables += 1;
                tt++;
                parent.seat(type);
                if (zeroInd > -1) {
                    tsa[zeroInd] += 1;
                } else {
                    int[] ntsa = new int[tsa.length + 1];
                    System.arraycopy(tsa, 0, ntsa, 0, tsa.length);
                    ntsa[tsa.length] = 1;
                    tableMap.put(type, ntsa);
                    tsa = ntsa;
                }
            }
        }
    }

    public void sampleCustomer(int type, int table) {
        double d = discount.doubleVal();
        double c = concentration.doubleVal();
        int[] tsa = tableMap.get(type);
        int tt = 0;
        int tc = -1;

        int zeroInd = -1;
        for (int i = 0; i < tsa.length; i++) {
            tc += tsa[i];
            if (tsa[i] > 0) {
                tt++;
            } else {
                zeroInd = i;
            }
        }

        tsa[table] -= 1;
        if (tsa[table] == 0) {
            parent.unseat(type);
            tables -= 1;
            tt--;
            zeroInd = table;
        }

        double r = RNG.nextDouble();
        double cuSum = 0.0;
        double pp = parent.predictiveProbability(type);
        double totalWeight = tc - tt * d + (tables * d + c) * pp;

        for (int i = 0; i < tsa.length; i++) {
            if (tsa[i] > 0) {
                cuSum += (tsa[i] - d) / totalWeight;
            }
            if (cuSum > r) {
                tsa[i] += 1;
                break;
            }
        }

        if (cuSum <= r) {
            tables += 1;
            if (zeroInd > -1) {
                tsa[zeroInd] += 1;
            } else {
                int[] ntsa = new int[tsa.length + 1];
                System.arraycopy(tsa, 0, ntsa, 0, tsa.length);
                ntsa[tsa.length] = 1;
                tableMap.put(type, ntsa);
            }
            parent.seat(type);
        }
    }

    public void sampleSeatingArrangements() {
        int[] typeTable = new int[2];
        RandomCustomer randomCustomer = new RandomCustomer(tableMap);
        while (randomCustomer.nextCustomer(typeTable) > -1) {
            sampleCustomer(typeTable[0], typeTable[1]);
        }

        fixZeros();
    }

    private void fixZeros() {
        for (Integer type : tableMap.keySet()) {
            int[] tsa = tableMap.get(type);

            int numZeros = 0;
            for (int tableSize : tsa) {
                assert (tableSize >= 0);
                if (tableSize == 0) {
                    numZeros++;
                }
            }

            if (numZeros > 0) {
                int[] ntsa = new int[tsa.length - numZeros];
                int ind = 0;
                for (int tableSize : tsa) {
                    if (tableSize > 0) {
                        ntsa[(ind++)] = tableSize;
                    }
                }
                tableMap.put(type, ntsa);
            }
        }
    }

    public double logLik() {
        int c = 0;
        int t = 0;
        double logLik = 0.0;

        for (int[] tsa : tableMap.values()) {
            for (int custs : tsa) {
                logLik += logLikTable(custs, c, t);
                t++;
                c += custs;
            }
        }

        return logLik;
    }

    private double logLikTable(int tableSize, int existingCust, int existingTables) {
        double disc = discount.doubleVal();
        double conc = concentration.doubleVal();
        double logLik = 0.0;
        double p = (existingTables * disc + conc) / (existingCust + conc);

        logLik += Math.log(p);
        existingCust++;

        for (int i = 1; i < tableSize; i++) {
            p = (i - disc) / (existingCust + conc);
            logLik += Math.log(p);
            existingCust++;
        }

        return logLik;
    }

    public void printTableMap() {
        for (Integer i : tableMap.keySet()) {
            int[] tsa = (int[]) tableMap.get(i);
            System.out.print(i + " : [" + tsa[0]);
            for (int j = 1; j < tsa.length; j++) {
                System.out.print(", " + tsa[j]);
            }
            System.out.println("]");
        }
    }

    private class RandomCustomer {

        private int[] randomType;
        private int[] randomTable;
        private int index;

        public RandomCustomer(TreeMap<Integer, int[]> tblMap) {
            index = 0;
            randomType = new int[customers];
            randomTable = new int[customers];

            int[] randomOrder = Util.randPermute(customers);

            int ind = 0;

            for (Integer type : tblMap.keySet()) {
                int[] tsa = (int[]) tblMap.get(type);
                for (int table = 0; table < tsa.length; table++) {
                    for (int customer = 0; customer < tsa[table]; customer++) {
                        randomType[randomOrder[ind]] = type;
                        randomTable[randomOrder[(ind++)]] = table;
                    }
                }
            }
        }

        public int nextCustomer(int[] typeAndTable) {
            if (index == customers) {
                return -1;
            }
            typeAndTable[0] = randomType[index];
            typeAndTable[1] = randomTable[(index++)];
            return 0;
        }
    }
}
