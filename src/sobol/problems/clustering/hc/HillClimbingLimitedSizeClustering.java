package sobol.problems.clustering.hc;

import sobol.base.random.RandomGeneratorFactory;
import sobol.base.random.generic.AbstractRandomGenerator;
import sobol.problems.clustering.generic.model.Project;

import java.io.PrintWriter;
import java.util.*;

/**
 * Hill Climbing algorithm with a maximum/minimum interval for each cluster size.
 */
public class HillClimbingLimitedSizeClustering extends HillClimbingClustering {

    private int minClusterSize;
    private int maxClusterSize;
    private int clusterCount;
    private AbstractRandomGenerator random;

    /**
     * Initializes the Hill Climbing search process
     *
     * @param detailsFile File where details of the search process will be saved
     * @param project Project whose classes will be distributed into clusters
     * @param maxEvaluations Budget of fitness evaluations
     * @param minClusterSize Minimum size of each cluster
     * @param maxClusterSize Maximum size of each cluster
     */
    public HillClimbingLimitedSizeClustering(PrintWriter detailsFile, Project project, int maxEvaluations, int minClusterSize, int maxClusterSize) throws Exception {
        super(detailsFile, project, maxEvaluations);

        if(minClusterSize > maxClusterSize) {
            throw new IllegalArgumentException("minClusterSize cannot be bigger than maxClusterSize");
        } else if(classCount < minClusterSize || classCount < maxClusterSize) {
            throw new IllegalArgumentException("Impossible to create clusters with given minClusterSize/maxClusterSize. classCount too small");
        }

        this.minClusterSize = minClusterSize;
        this.maxClusterSize = maxClusterSize;
        random = RandomGeneratorFactory.createForPopulation(classCount);
    }

    /**
     * Initializes the Hill Climbing search process
     *
     * @param project Project whose classes will be distributed into clusters
     * @param maxEvaluations Budget of fitness evaluations
     * @param minClusterSize Minimum size of each cluster
     * @param maxClusterSize Maximum size of each cluster
     */
    public HillClimbingLimitedSizeClustering(Project project, int maxEvaluations, int minClusterSize, int maxClusterSize) throws Exception {
        this(null, project, maxEvaluations, minClusterSize, maxClusterSize);
    }

    /**
     * Executes the Hill Climbing search with random restarts
     */
    @Override
    public int[] execute() throws Exception
    {
        this.bestSolution = generateSolution();
        applySolution(bestSolution);
        this.fitness = evaluate();


        int[] solution = new int[classCount];
        copySolution(bestSolution, solution);

        while (localSearch(solution))
        {
            this.randomRestartCount++;
            solution = generateSolution();
        }

        return bestSolution;
    }

    /**
     * Generates an initial solution respecting the minClusterSize and maxClusterSize constraints.
     */
    private int[] generateSolution() {
        int[] solution = new int[classCount];
        int numberOfClusters = selectNumberOfClusters(classCount, minClusterSize, maxClusterSize);
        this.clusterCount = numberOfClusters;
        List<Integer> listOfClasses = generateIntegerListWithSize(classCount);
        Map<Integer, Integer> clusters = generateEmptyClusters(numberOfClusters);

        //select minClusterSize classes for each cluster
        for(Map.Entry<Integer, Integer> cluster : clusters.entrySet()) {
            while(cluster.getValue() < minClusterSize) {
                int position = random.singleInt(0, listOfClasses.size()-1);
                Integer clazz = listOfClasses.remove(position);
                solution[clazz] = cluster.getKey();
                cluster.setValue(cluster.getValue()+1);
            }
        }

        //select a cluster for each remaining class
        for (Integer clazz : listOfClasses) {
            int cluster = selectRandomCluster(clusters, maxClusterSize);
            solution[clazz] =  cluster;
            clusters.put(cluster, clusters.get(cluster)+1);
        }

        return solution;
    }

    /**
     * Return a cluster that is not full yet.
     */
    private int selectRandomCluster(Map<Integer, Integer> clusters, int maxClusterSize) {
        List<Integer> clustersNotFull = new LinkedList<Integer>();

        for(Map.Entry<Integer,Integer> cluster : clusters.entrySet()) {
            if(cluster.getValue() < maxClusterSize)  {
                clustersNotFull.add(cluster.getKey());
            }
        }

        int position = random.singleInt(0, clustersNotFull.size()-1);
        return clustersNotFull.get(position);
    }

    /**
     * Generate the effective maximum/minimum cluster sized based on number of classes
     * of a instance.
     */
    private int selectNumberOfClusters(int classCount, int minClusterSize, int maxClusterSize) {
        int maxAllowedNumber = classCount;
        int minAllowedSize = 1;

        while((maxAllowedNumber * minClusterSize) > classCount)
            maxAllowedNumber--;

        while ((minAllowedSize * maxClusterSize) < classCount)
            minAllowedSize++;

        return random.singleInt(minAllowedSize, maxAllowedNumber);
    }

    /**
     * Return a sequential list going from 0 to classCount-1
     */
    private List<Integer> generateIntegerListWithSize(int classCount) {
        List<Integer> list = new LinkedList<Integer>();
        for(int idx = 0; idx < classCount; idx++) {
            list.add(idx);
        }

        return list;
    }

    /**
     * Return a map with 'number' empty clusters.
     */
    private Map<Integer, Integer> generateEmptyClusters(int number) {
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        for(int idx = 0; idx < number; idx++) {
            map.put(idx, 0);
        }

        return map;
    }

    /**
     * Runs a neighborhood visit starting from a given solution
     */
    @Override
    protected NeighborhoodVisitorResult visitNeighbors(int[] solution)
    {
        applySolution(solution);
        double startingFitness = evaluate();

        if (evaluations > maxEvaluations)
            return new NeighborhoodVisitorResult(NeighborhoodVisitorStatus.SEARCH_EXHAUSTED);

        if (startingFitness > fitness)
            return new NeighborhoodVisitorResult(NeighborhoodVisitorStatus.FOUND_BETTER_NEIGHBOR, startingFitness);

        for (int i = 0; i < classCount; i++)
        {
            for (int j = 0; j < clusterCount; j++)
            {
                if (solution[i] != j)
                {
                    calculator.moveClass(i, j);
                    int[] tmpSolution = calculator.getSolution();

                    if(isSolutionValid(tmpSolution)) {
                        double neighborFitness = evaluate();

                        if (evaluations > maxEvaluations)
                            return new NeighborhoodVisitorResult(NeighborhoodVisitorStatus.SEARCH_EXHAUSTED);

                        if (neighborFitness > startingFitness)
                        {
                            solution[i] = j;
                            return new NeighborhoodVisitorResult(NeighborhoodVisitorStatus.FOUND_BETTER_NEIGHBOR, neighborFitness);
                        }
                        else
                            calculator.moveClass(i, solution[i]);
                    }
                    else
                    {
                        calculator.moveClass(i, solution[i]);
                    }
                }
            }
        }

        return new NeighborhoodVisitorResult(NeighborhoodVisitorStatus.NO_BETTER_NEIGHBOR);
    }

    /**
     * Checks if a given solution satisfies the size constraints regarding clusters' size
     */
    private boolean isSolutionValid(int[] solution) {
        Map<Integer, Integer> sizeOfClusters = getSizeOfEachCluster(solution);

        for (Integer size : sizeOfClusters.values()) {
            if (size < this.minClusterSize || size > this.maxClusterSize) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns a map with size of each cluster in solution.
     */
    private Map<Integer, Integer> getSizeOfEachCluster(int[] solution) {
        Map<Integer, Integer> sizeOfClusters = new HashMap<Integer, Integer>();

        for (int clusterNumber : solution) {
            Integer numberOfClasses = sizeOfClusters.get(clusterNumber);

            if(numberOfClasses == null) {
                numberOfClasses = 1;
            } else {
                numberOfClasses++;
            }

            sizeOfClusters.put(clusterNumber, numberOfClasses);
        }
        return sizeOfClusters;
    }
}
