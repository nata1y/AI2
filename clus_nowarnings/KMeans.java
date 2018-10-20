import java.util.*;
import java.util.Collections;

public class KMeans extends ClusteringAlgorithm
{
	// Number of clusters
	private int k;

	// Dimensionality of the vectors
	private int dim;
	
	// Threshold above which the corresponding html is prefetched
	private double prefetchThreshold;
	
	// Array of k clusters, class cluster is used for easy bookkeeping
	private Cluster[] clusters;
	
	// This class represents the clusters, it contains the prototype (the mean of all it's members)
	// and memberlists with the ID's (which are Integer objects) of the datapoints that are member of that cluster.
	// You also want to remember the previous members so you can check if the clusters are stable.
	static class Cluster
	{
		float[] prototype;

		Set<Integer> currentMembers;
		Set<Integer> previousMembers;
		  
		public Cluster()
		{
			currentMembers = new HashSet<Integer>();
			previousMembers = new HashSet<Integer>();
		}
	}
	// These vectors contains the feature vectors you need; the feature vectors are float arrays.
	// Remember that you have to cast them first, since vectors return objects.
	private Vector<float[]> trainData;
	private Vector<float[]> testData;

	// Results of test()
	private double hitrate;
	private double accuracy;
	
	public KMeans(int k, Vector<float[]> trainData, Vector<float[]> testData, int dim)
	{
		this.k = k;
		this.trainData = trainData;
		this.testData = testData; 
		this.dim = dim;
		prefetchThreshold = 0.5;
		
		// Here k new cluster are initialized
		clusters = new Cluster[k];
		for (int ic = 0; ic < k; ic++)
			clusters[ic] = new Cluster();
	}

	public Double euclidian_distance(Cluster cluster, float[] dataVec){
		Double distance = new Double(0);
		for(int i = 0; i < dataVec.length; i++){
			distance += Math.pow(dataVec[i] - cluster.prototype[i], 2); 
		}
		return (Double)Math.sqrt(distance);
	}

	public int findPrototype(float[] dataVec){
		Double[] dists = new Double[clusters.length];
		for(int i = 0; i < clusters.length; i++){
			dists[i] = euclidian_distance(clusters[i], dataVec);
		}

		List help = Arrays.asList(dists);
		Double minDist = (Double)Collections.min(help);
		int idx = help.indexOf(minDist);
		return idx;
	}

	/// compute mean for one cluster
	public float[] computeMean(Cluster clust){
		float[] mean = new float[dim];
		for(int i = 0; i < dim; i ++){
			mean[i] = 0;
			Iterator iterator = clust.currentMembers.iterator();
			while(iterator.hasNext()){
				mean[i] += trainData.get((int)iterator.next())[i];
			}
			mean[i] /= clust.currentMembers.size();
		}
		return mean;
	}

	public void oneEpoch(){
		int[] correspondPrtotypes = new int[trainData.size()];

		for(int i = 0; i < clusters.length; i ++){
			clusters[i].previousMembers = new HashSet<Integer>();
			Iterator iterator = clusters[i].currentMembers.iterator();
			while(iterator.hasNext()){
				clusters[i].previousMembers.add((Integer)iterator.next());
			}
			clusters[i].currentMembers = new HashSet<Integer>();
		}
		/// finding corresponding cluster (prototype) for each data vector
		/// reassigning datavectors to new clusters step 2
		for(int i = 0; i < trainData.size(); i++){
			correspondPrtotypes[i] = findPrototype(trainData.get(i));
			clusters[correspondPrtotypes[i]].currentMembers.add(i);
		}

		/// compute new centroid step 3
		for(int i = 0; i < clusters.length; i++){
			clusters[i].prototype = computeMean(clusters[i]);
		}
	}

	public boolean checkCond(){
		for(int i = 0; i < clusters.length; i ++){
				Iterator iterator = clusters[i].currentMembers.iterator();
				Iterator iterator2 = clusters[i].previousMembers.iterator();
				while(iterator.hasNext() && iterator2.hasNext()){
					if (iterator.next() != iterator2.next()){
						return true;
					}
				}
				if (iterator2.hasNext() || iterator.hasNext()){
					return true;
				}
		}
		return false;
	}
	
	public boolean train()
	{
	 	//implement k-means algorithm here:
		// Step 1: Select an initial random partioning with k clusters
		// Step 2: Generate a new partition by assigning each datapoint to its closest cluster center
		// Step 3: recalculate cluster centers
		// Step 4: repeat until clustermembership stabilizes

		///Step 1
		Collections.shuffle(trainData);
		boolean condition = true;

		for(int n = 0; n < k; n++){
			for(int i=(int)(n*trainData.size()/k); i<(int)((n+1)*trainData.size()/k); i++) {
				clusters[n].currentMembers.add(i);
				clusters[n].previousMembers.add(0);
			}
		}

		for(int i = 0; i<clusters.length; i++){
			clusters[i].prototype = computeMean(clusters[i]);
		}

		while(condition){
			//System.out.println(condition);
			oneEpoch();
			/// step 4
			condition = checkCond();
		}

		return false;
	}

	public boolean test()
	{
		// iterate along all clients. Assumption: the same clients are in the same order as in the testData
		// for each client find the cluster of which it is a member
		// get the actual testData (the vector) of this client
		// iterate along all dimensions
		// and count prefetched htmls
		// count number of hits
		// count number of requests
		// set the global variables hitrate and accuracy to their appropriate value
		double requests = 0;
		double hits = 0;
		double prefetches = 0;
		for(int i = 0; i < testData.size(); i++){
			float[] client = testData.get(i);
			int idx = findPrototype(client);
			float[] prot = clusters[idx].prototype;
			for(int j = 0; j < dim; j++){
				if(prot[j] > prefetchThreshold){
					prefetches++;
				}
				if(prot[j] > prefetchThreshold && client[j] == 1){
					hits ++;
				}
				requests += client[j];
			}

		}
		accuracy = hits/prefetches;
		hitrate = hits/requests;
		return true;
	}


	// The following members are called by RunClustering, in order to present information to the user
	public void showTest()
	{
		System.out.println("Prefetch threshold=" + this.prefetchThreshold);
		System.out.println("Hitrate: " + this.hitrate);
		System.out.println("Accuracy: " + this.accuracy);
		System.out.println("Hitrate+Accuracy=" + (this.hitrate + this.accuracy));
	}
	
	public void showMembers()
	{
		for (int i = 0; i < k; i++)
			System.out.println("\nMembers cluster["+i+"] :" + clusters[i].currentMembers);
	}
	
	public void showPrototypes()
	{
		for (int ic = 0; ic < k; ic++) {
			System.out.print("\nPrototype cluster["+ic+"] :");
			
			for (int ip = 0; ip < dim; ip++)
				System.out.print(clusters[ic].prototype[ip] + " ");
			
			System.out.println();
		 }
	}

	// With this function you can set the prefetch threshold.
	public void setPrefetchThreshold(double prefetchThreshold)
	{
		this.prefetchThreshold = prefetchThreshold;
	}
}
