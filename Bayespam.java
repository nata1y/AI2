import java.io.*;
import java.util.*;

public class Bayespam
{

    /// 2.2 epsilon 
    final static float epsilon = 1;

    // This defines the two types of messages we have.
    static enum MessageType
    {
        NORMAL, SPAM
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
    private static Hashtable <String, Multiple_Counter> vocab = new Hashtable <String, Multiple_Counter> ();

    private static Hashtable <String, Probabilities> word_prob = new Hashtable <String, Probabilities> ();
    private static Hashtable <String, Probabilities> message_prob = new Hashtable <String, Probabilities> ();

    private static ArrayList<String> messages = new ArrayList<String>();

    // Add a word to the vocabulary
    private static void addWord(String word, MessageType type)
    {
        Multiple_Counter counter = new Multiple_Counter();

        if ( vocab.containsKey(word) ){                  // if word exists already in the vocabulary..
            counter = vocab.get(word);                  // get the counter from the hashtable
        }
        counter.incrementCounter(type);                 // increase the counter appropriately

        vocab.put(word, counter);                       // put the word with its counter into the hashtable
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
    private static void printVocab()
    {
        Multiple_Counter counter = new Multiple_Counter();

        for (Enumeration<String> e = vocab.keys() ; e.hasMoreElements() ;)
        {   
            String word;
            
            word = e.nextElement();
            counter  = vocab.get(word);
            
            System.out.println( word + " | in regular: " + counter.counter_regular + 
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
            String word;
            
            while ((line = in.readLine()) != null)                      // read a line
            {
                StringTokenizer st = new StringTokenizer(line);         // parse it into words
        
                /// clean volcabulary
                while (st.hasMoreTokens())                  // while there are still words left..
                {
                	String instring = st.nextToken().toString().toLowerCase();
                	if(instring.length() >= 4 && instring.matches("[a-z]+")) {
                        addWord(instring, type);                  // add them to the vocabulary
                	}
                }
            }

            in.close();
        }
    }

    private static void readMessages(File dir_location)
    throws IOException
    {

        File[] dir_listing = dir_location.listFiles();

        // Check that there are 2 subdirectories
        if ( dir_listing.length != 2 )
        {
            System.out.println( "- Error: specified directory does not contain two subdirectories.\n" );
            Runtime.getRuntime().exit(0);
        }

        File[] files = dir_listing[0].listFiles();
        
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

        // Print out the hash table
        printVocab();

        /// calculate priors
        double prior_reg_mes = listing_regular.length/(listing_regular.length + listing_spam.length);
        double prior_spam_mes = listing_spam.length/(listing_regular.length + listing_spam.length);

        double den = 0;

        /// calculate total number of words
        Set<String> keys = vocab.keySet();

        for(String key: keys){
            den += (vocab.get(key).counter_spam + vocab.get(key).counter_regular);
        }

        for(String key: keys){
            Probabilities thisWord = new Probabilities(Math.log((vocab.get(key).counter_spam)/den), Math.log((vocab.get(key).counter_regular)/den));
            word_prob.put(key, thisWord);
        }

        ///classifying message 3.1

        File dir_classify_location = new File( args[1] );
        
        // Check if the cmd line arg is a directory
        if ( !dir_classify_location.isDirectory() )
        {
            System.out.println( "- Error: cmd line arg not a directory.\n" );
            Runtime.getRuntime().exit(0);
        }
        readMessages(dir_classify_location);


        /// print probs
        /*
        for(String key: keys){
            System.out.println(key + "  " + word_prob.get(key).getCond_spam());
        }*/

        /// Check print messages
        /*
        for(String i: messages){
            System.out.println(i);
        }*/

        double overall_amount = listing_regular.length + listing_spam.length;
        double current_p_r;
        double current_p_s;
        for(String mes: messages){
            current_p_r = Math.log(prior_reg_mes);
            current_p_s = Math.log(prior_spam_mes);
            
            StringTokenizer st = new StringTokenizer(mes);         // parse it into words
        
                /// clean volcabulary
                while (st.hasMoreTokens())                  // while there are still words left..
                {
                	String instring = st.nextToken().toString().toLowerCase();
                	if(instring.length() >= 4 && instring.matches("[a-z]+") && word_prob.containsKey(instring)) {
                        ///HERE WE NEED TO SEARCH FOR PROBABILITIES
                        current_p_r += Math.log(word_prob.get(instring).getCond_reg());
                        current_p_s += Math.log(word_prob.get(instring).getCond_spam());
                	}
                }

            Probabilities thisWord = new Probabilities(current_p_s, current_p_r);
            message_prob.put(mes, thisWord);
        }

        keys = message_prob.keySet();
        for(String key: keys){
            System.out.println(message_prob.get(key).getCond_reg());
        }

        
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