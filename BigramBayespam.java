import java.io.*;
import java.util.*;

public class BigramBayespam
{

    /// 2.2 epsilon 
    final static float epsilon = 1;

    // This defines the two types of messages we have.
    static enum MessageType
    {
        NORMAL, SPAM
    }

    static class Pair 
    {
        private String word1;
        private String word2;
        private String key;

        public Pair(String word1, String word2) {
            this.word1 = word1;
            this.word2 = word2;
            this.key = word1+word2;
        }

        public void setWord1(String word1) {
            this.word1 = word1;
            this.key = word1+word2;

        }

        public void setWord2(String word2) {
            this.word2 = word2;
            this.key = word1+word2;
        }

        public String getWord1() {
            return word1;
        }

        public String getWord2() {
            return word2;
        }

        public String getKey() {
            return key;
        }

        public String toString() {
            return "(" + word1 + " - " + word2 + ")";
        }
    }

    static class Probabilities
    {
        private double cond_spam = epsilon;
        private double cond_reg = epsilon;

        public Probabilities(double spam, double reg){
            cond_spam = spam;
            cond_reg = reg;
        }

        public double getCond_reg() {
            return cond_reg;
        }

        public double getCond_spam() {
            return cond_spam;
        }

        public void setCond_reg(double cond_reg) {
            this.cond_reg = cond_reg;
        }

        public void setCond_spam(double cond_spam) {
            this.cond_spam = cond_spam;
        }
    }

    // This a class with two counters (for regular and for spam)
    static class Multiple_Counter
    {
        int counter_spam    = 0;
        int counter_regular = 0;

        // Increase one of the counters by one
        public void incrementCounter(MessageType type)
        {
            if ( type == MessageType.NORMAL ){
                ++counter_regular;
            } else {
                ++counter_spam;
            }
        }
    }

    // Listings of the two subdirectories (regular/ and spam/)
    private static File[] listing_regular = new File[0];
    private static File[] listing_spam = new File[0];

    // A hash table for the vocabulary (word searching is very fast in a hash table)
    private static Hashtable <Pair, Multiple_Counter> bigrams = new Hashtable<Pair, Multiple_Counter>();

    private static Hashtable <Pair, Probabilities> bigram_prob = new Hashtable <Pair, Probabilities> ();
    private static Hashtable <String, Probabilities> message_prob = new Hashtable <String, Probabilities> ();

    private static ArrayList<String> messages = new ArrayList<String>();

    private static void addPair(Pair pair, MessageType type) {
        Multiple_Counter counter = new Multiple_Counter();

        for(Pair storedPair : bigrams.keySet()) {
            if(storedPair.getWord1().equals(pair.getWord1()) && storedPair.getWord2().equals(pair.getWord2())) {
                counter = bigrams.get(storedPair);
                bigrams.remove(storedPair);
                break;
            }
        }
        counter.incrementCounter(type);
        bigrams.put(pair, counter);
    }


    // List the regular and spam messages
    private static void listDirs(File dir_location)
    {
        // List all files in the directory passed
        File[] dir_listing = dir_location.listFiles();

        // Check that there are 2 subdirectories
        if ( dir_listing.length != 2 )
        {
            System.out.println( "- Error: specified directory does not contain two subdirectories.\n" );
            Runtime.getRuntime().exit(0);
        }
        listing_regular = dir_listing[0].listFiles();
        listing_spam    = dir_listing[1].listFiles();
    }

    
    // Print the current content of the vocabulary
    private static void printBigrams()
    {
        Multiple_Counter counter = new Multiple_Counter();

        for (Enumeration<Pair> e = bigrams.keys() ; e.hasMoreElements() ;)
        {   
            Pair pair;
            
            pair = e.nextElement();
            counter = bigrams.get(pair);
            
            System.out.println( pair.toString() + " | in regular: " + counter.counter_regular + 
                                " in spam: "    + counter.counter_spam);
        }
    }

    // Read the words from messages and add them to your vocabulary. The boolean type determines whether the messages are regular or not  
    private static void readMessages(MessageType type)
    throws IOException
    {
        File[] messages = new File[0];

        if (type == MessageType.NORMAL){
            messages = listing_regular;
        } else {
            messages = listing_spam;
        }
        
        for (int i = 0; i < messages.length; ++i)
        {
            FileInputStream i_s = new FileInputStream( messages[i] );
            BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
            String line;
            String prevWord = "";
            while ((line = in.readLine()) != null)                      // read a line
            {
                StringTokenizer st = new StringTokenizer(line);         // parse it into words
                /// clean volcabulary
                while (st.hasMoreTokens())                  // while there are still words left..
                {
                	String instring = st.nextToken().toString().toLowerCase();
                	if(instring.length()>4 && instring.matches("[a-z]+")) { // 
                        //addWord(instring, type);                  // add them to the vocabulary
                        if(prevWord!="") addPair(new Pair(prevWord.toLowerCase(), instring.toLowerCase()), type);
                        prevWord = instring;
                	}
                }
            }
            in.close();
        }
    }

    /// Is used to read all messages one-by-one from a given directory to the array list if strings messages.
    /// Every entry in the array is a string representation of content of corresponding message file.
    private static void readMessages(File dir_location)
    throws IOException
    {
        File[] files = dir_location.listFiles();
        
        for (int i = 0; i < files.length; ++i)
        {
            FileInputStream i_s = new FileInputStream( files[i] );
            BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
            String line;
            String message = "";
            
            while ((line = in.readLine()) != null)                      // read a line
            {
                message += line;
            }
            message += "\n";
            messages.add(message);

            in.close();
        }
    }

   
    public static void main(String[] args)
    throws IOException
    {
        // Location of the directory (the path) taken from the cmd line (first arg)
        File dir_location = new File( args[0] );
        
        // Check if the cmd line arg is a directory
        if ( !dir_location.isDirectory() )
        {
            System.out.println( "- Error: cmd line arg not a directory.\n" );
            Runtime.getRuntime().exit(0);
        }

        // Initialize the regular and spam lists
        listDirs(dir_location);

        // Read the e-mail messages
        readMessages(MessageType.NORMAL);
        readMessages(MessageType.SPAM);

        /// calculate priors
        double prior_reg_mes = (double)listing_regular.length/(double)(listing_regular.length + listing_spam.length);
        double prior_spam_mes = (double)listing_spam.length/(double)(listing_regular.length + listing_spam.length);

        double den_reg = 0;
        double den_spam = 0;
        double prob_reg = 0;
        double prob_spam = 0;

        /// calculate total number of words
        Set<Pair> keys = bigrams.keySet();

        /// calculate total word numbers for both - spam and regular
        for(Pair key: keys){
            den_reg += bigrams.get(key).counter_regular;
            den_spam += bigrams.get(key).counter_spam;
        }

        List<Pair> removeList = new ArrayList<Pair>();

        /// calculate probabilities for a particular word to be spam or to be regular
        for(Pair key: keys){
            double num_spam = bigrams.get(key).counter_spam;
            double num_reg = bigrams.get(key).counter_regular;
            if(Math.abs(num_spam-num_reg)<5) {
               removeList.add(key); 
            }
            else {
                if(num_spam == 0){
                    num_spam = epsilon;
                    prob_spam = Math.log(num_spam/(den_spam + den_reg));
                } else {
                    prob_spam = Math.log(num_spam/den_spam);
                }
                if (num_reg == 0){
                    num_reg = epsilon;
                    prob_reg = Math.log(num_reg/(den_spam + den_reg));
                } else {
                    prob_reg = Math.log(num_reg/den_reg);
                }
                Probabilities thisWord = new Probabilities(prob_spam, prob_reg);
                bigram_prob.put(key, thisWord);
            }
            
        }
        while(removeList!=null && removeList.size()>0) {
            bigrams.remove(removeList.get(0));
            removeList.remove(0);
        }

        // Print out the hash table
        printBigrams();
        ///classifying message 3.1

        File dir_classify_location = new File( args[1] );
        
        // Check if the cmd line arg is a directory
        if ( !dir_classify_location.isDirectory() )
        {
            System.out.println( "- Error: cmd line arg not a directory.\n" );
            Runtime.getRuntime().exit(0);
        }
        readMessages(dir_classify_location);

        double current_p_r;
        double current_p_s;

        /// go through all given messages
        for(String mes: messages){
            current_p_r = Math.log(prior_reg_mes);
            current_p_s = Math.log(prior_spam_mes);
            
            StringTokenizer st = new StringTokenizer(mes);         // parse it into words
        
                /// clean volcabulary
                /// for every message go through every word and calculate probabilities
                String prevWord = "";
                while (st.hasMoreTokens())                  // while there are still words left..
                {
                    String instring = st.nextToken().toString().toLowerCase();
                	if(instring.length()>4 && instring.matches("[a-z]+")){  // 
                        
                        if(prevWord!="") {
                            Pair pair = new Pair(prevWord.toLowerCase(), instring.toLowerCase());
                            for(Pair storedPair : bigram_prob.keySet()) {
                                
                                if(pair.toString().equals(storedPair.toString())) {
                                    
                                    current_p_r += bigram_prob.get(storedPair).getCond_reg();
                                    current_p_s += bigram_prob.get(storedPair).getCond_spam();
                                }
                            }
                        }
                        prevWord = instring;
                        ///HERE WE NEED TO SEARCH FOR PROBABILITIES
                        /// already logs!
                	}
                }

            /// save probabilities
            Probabilities thisWord = new Probabilities(current_p_s, current_p_r);
            message_prob.put(mes, thisWord);
        }

        /// decide on message type
        int am_reg_in_reg = 0;
        int am_spam_in_reg = 0;

        Set<String> messKeys = message_prob.keySet();
        for(String key: messKeys){
            if (message_prob.get(key).getCond_reg() > message_prob.get(key).getCond_spam()){
                System.out.println("Regular");
                am_reg_in_reg += 1;
            } else {
                System.out.println("Spam");
                am_spam_in_reg += 1;
            }
        }

        System.out.println("Spam in given text= " + am_spam_in_reg);
        System.out.println("Reg in given text= " + am_reg_in_reg);
        System.out.println("Overall= " + (am_spam_in_reg + am_reg_in_reg));

        /// computing performance of the test

        
        // Now all students must continue from here:
        //
        // 1) A priori class probabilities must be computed from the number of regular and spam messages
        // 2) The vocabulary must be clean: punctuation and digits must be removed, case insensitive
        // 3) Conditional probabilities must be computed for every word
        // 4) A priori probabilities must be computed for every word
        // 5) Zero probabilities must be replaced by a small estimated value
        // 6) Bayes rule must be applied on new messages, followed by argmax classification
        // 7) Errors must be computed on the test set (FAR = false accept rate (misses), FRR = false reject rate (false alarms))
        // 8) Improve the code and the performance (speed, accuracy)
        //
        // Use the same steps to create a class BigramBayespam which implements a classifier using a vocabulary consisting of bigrams
    }
}