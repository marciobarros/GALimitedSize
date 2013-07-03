package sobol.problems.clustering.hc;

import sobol.base.random.RandomGeneratorFactory;
import sobol.base.random.generic.AbstractRandomGenerator;
import sobol.problems.clustering.generic.model.Project;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Hill Climbing algorithm with a maximum/minimum interval for each cluster size.
 */
public class HillClimbingLimitedSizeClustering extends HillClimbingClustering {

    private int minClusterSize;
    private int maxClusterSize;

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
        }

        this.minClusterSize = minClusterSize;
        this.maxClusterSize = maxClusterSize;
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
        AbstractRandomGenerator random = RandomGeneratorFactory.createForPopulation(classCount);

        this.bestSolution = random.randInt(0, packageCount - 1);
        applySolution(bestSolution);
        this.fitness = evaluate();

        int[] solution = new int[classCount];
        copySolution(bestSolution, solution);

        while (localSearch(solution))
        {
            this.randomRestartCount++;
            solution = random.randInt(0, packageCount - 1);
        }

        return bestSolution;
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
            for (int j = 0; j < packageCount; j++)
            {
                if (solution[i] != j)
                {
                    calculator.moveClass(i, j);
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
