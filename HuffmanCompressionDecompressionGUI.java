import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.filechooser.FileNameExtensionFilter;

// Node class represents a node in the Huffman tree
class Node {
    char data; // The character stored in the node (only for leaf nodes)
    int frequency; // Frequency of the character or sum of frequencies for internal nodes
    Node left, right; // Left and right children in the Huffman tree

    // Constructor for leaf nodes (nodes containing actual characters)
    Node(char data, int frequency) {
        this.data = data; // Assign the character
        this.frequency = frequency; // Assign the frequency
        left = right = null;
    }

    // Constructor for internal nodes (no character, only combined frequency)
    Node(int frequency) {
        this.frequency = frequency; // Assign the combined frequency
        left = right = null;
    }
}

public class HuffmanCompressionDecompressionGUI extends JFrame {

    private static final int MAX_CHAR = 65536; // Unicode range
    private int[] frequencyTable = new int[MAX_CHAR]; // Store frequencies
    private String[] huffmanCodes = new String[MAX_CHAR];
    private Node[] heap = new Node[MAX_CHAR]; // Min-heap array
    private int heapSize = 0;
    private int charCount = 0; // Number of unique characters
    private int count = 0;
    private JTextArea outputTextArea;

    public HuffmanCompressionDecompressionGUI() {
        // Set up the GUI window title and dimensions
        setTitle("Huffman Compression/Decompression Tool");
        setSize(600, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close the app when window is closed
        setLocationRelativeTo(null); // Center the window on the screen

        // Main panel with BorderLayout to organize different GUI sections
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(240, 248, 255)); // Light blue backgro

        // Header Label
        JLabel titleLabel = new JLabel("Huffman Compression/Decompression");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(new Color(25, 25, 112)); // Midnight blue
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Buttons Panel to hold the compress and decompress buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(new Color(240, 248, 255)); // Match background color

        // Create styled buttons for compression and decompression
        JButton compressButton = createStyledButton("Compress File", new Color(70, 130, 180)); // Steel blue
        JButton decompressButton = createStyledButton("Decompress File", new Color(60, 179, 113)); // Medium sea green

        // Add the buttons to the button panel
        buttonPanel.add(compressButton);
        buttonPanel.add(decompressButton);

        // Output text area to display process logs and results
        outputTextArea = new JTextArea(12, 40);
        outputTextArea.setEditable(false); // Make it read-only
        outputTextArea.setFont(new Font("Courier New", Font.PLAIN, 13)); // Monospaced font
        outputTextArea.setForeground(new Color(25, 25, 25)); // Text color
        outputTextArea.setBackground(new Color(245, 245, 245)); // Light gray background
        outputTextArea.setBorder(BorderFactory.createLineBorder(new Color(70, 130, 180), 2)); // Blue border

        // Put the text area inside a scroll pane to allow scrolling
        JScrollPane scrollPane = new JScrollPane(outputTextArea);
        mainPanel.add(scrollPane, BorderLayout.SOUTH); // Add scrollable text area at the bottom

        // Add the button panel to the center region of the layout
        mainPanel.add(buttonPanel, BorderLayout.CENTER);

        // Add the fully assembled main panel to the frame
        add(mainPanel);

        // Attach event listeners to the buttons to trigger compression or decompression
        compressButton.addActionListener(e -> handleCompressButtonClick());
        decompressButton.addActionListener(e -> handleDecompressButtonClick());
    }

    // Method to create a styled JButton with custom font, color, and size
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text); // Create button with given label
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false); // Remove default focus highlight
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setPreferredSize(new Dimension(200, 40));
        return button;
    }

    // Method to read a file and build the frequency table of characters
    private void buildFrequencyTable(String filePath) throws IOException {
        // Use BufferedReader for efficient character reading
        try (BufferedReader br = new BufferedReader(new FileReader(filePath), 8192)) {
            int ch;
            while ((ch = br.read()) != -1) { // Read each character
                if (frequencyTable[ch] == 0) {
                    charCount++; // If it's the first occurrence, count it as unique
                }
                frequencyTable[ch]++; // Increment frequency count
            }
        }
    }

    // Method to insert a node into the min-heap while maintaining heap order
    private void insertHeap(Node node) {
        heap[heapSize] = node; // Place node at the end of the heap
        int currentIndex = heapSize++; // Save current index and increase heap size

        // Bubble up the node until heap property is restored
        while (currentIndex > 0) {
            int parentIndex = (currentIndex - 1) / 2;

            // If the current node's frequency is greater than or equal to the parent's
            if (heap[currentIndex].frequency >= heap[parentIndex].frequency) {
                break;
            }

            // Otherwise, swap with parent and continue bubbling up
            swap(currentIndex, parentIndex);
            currentIndex = parentIndex;
        }
    }

    // Method to extract (remove and return) the node with the minimum frequency
    // from the heap
    private Node extractMin() {
        Node minNode = heap[0]; // Root of the heap is the minimum element
        heap[0] = heap[--heapSize]; // Replace root with last node and reduce heap size
        heapify(0); // Restore heap property from the root downward
        return minNode;
    }

    // Restores the min-heap property starting from a given index.
    private void heapify(int index) {
        int smallest = index;
        int leftChild = 2 * index + 1; // Index of left child
        int rightChild = 2 * index + 2; // Index of right child

        // Check if left child is smaller than the current smallest
        if (leftChild < heapSize && heap[leftChild].frequency < heap[smallest].frequency) {
            smallest = leftChild;
        }

        // Check if right child is smaller than the current smallest
        if (rightChild < heapSize && heap[rightChild].frequency < heap[smallest].frequency) {
            smallest = rightChild;
        }

        // If a smaller child is found, swap and continue heapifying
        if (smallest != index) {
            swap(smallest, index);
            heapify(smallest);
        }
    }

    // Swaps two nodes in the heap based on their indices
    private void swap(int i, int j) {
        Node temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
    }

    // Builds the Huffman Tree using the frequency table and min-heap
    private Node buildHuffmanTree() {
        // Insert all characters with non-zero frequency into the min-heap
        for (int i = 0; i < MAX_CHAR; i++) {
            if (frequencyTable[i] > 0) {
                insertHeap(new Node((char) i, frequencyTable[i]));
            }
        }

        // Repeat until only one node remains (the root of the tree)
        while (heapSize > 1) {
            // Extract the two nodes with the smallest frequencies
            Node node1 = extractMin();
            Node node2 = extractMin();

            // Create a new internal node with combined frequency
            Node newNode = new Node(node1.frequency + node2.frequency);
            newNode.left = node1; // Left child
            newNode.right = node2; // Right child

            // Insert the new internal node back into the heap
            insertHeap(newNode);
        }

        return heap[0]; // Return the root of the final Huffman Tree
    }

    // Recursively generates Huffman codes by traversing the Huffman Tree
    private void generateHuffmanCodes(Node node, String code) {
        // Base case: if it's a leaf node, assign the code to the character
        if (node.left == null && node.right == null) {
            huffmanCodes[node.data] = code;
            return;
        }

        // Traverse left child and append '0' to the code
        generateHuffmanCodes(node.left, code + "0");

        // Traverse right child and append '1' to the code
        generateHuffmanCodes(node.right, code + "1");
    }

    // Compresses the input file using Huffman encoding and writes to the output
    // binary file
    private void compressFile(String inputFilePath, String outputFilePath) throws IOException {
        // Open input file for reading and output file for writing in buffered mode
        try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath), 8192);
                DataOutputStream dos = new DataOutputStream(
                        new BufferedOutputStream(new FileOutputStream(outputFilePath), 8192))) {

            // Write the number of unique characters first (needed for decompression)
            dos.writeInt(charCount);

            // Write each character and its frequency to the output file
            for (int i = 0; i < MAX_CHAR; i++) {
                if (frequencyTable[i] > 0) {
                    dos.writeChar(i); // Write the character
                    dos.writeInt(frequencyTable[i]); // Write its frequency
                }
            }

            int bitBuffer = 0; // Buffer to hold bits before writing as a byte
            int bitCount = 0; // Number of bits currently in buffer
            int ch;

            // Read each character from input and encode using Huffman codes
            while ((ch = br.read()) != -1) {
                String code = huffmanCodes[ch]; // Get Huffman code for the character
                for (char bit : code.toCharArray()) {
                    bitBuffer <<= 1; // Left-shift to make room for the next bit
                    if (bit == '1') {
                        bitBuffer |= 1; // Set the least significant bit to 1 if needed
                    }
                    bitCount++; // Increase count of bits in buffer

                    // When buffer reaches 8 bits (1 byte), write it to output
                    if (bitCount == 8) {
                        dos.write(bitBuffer); // Write one byte
                        bitBuffer = 0; // Reset buffer
                        bitCount = 0; // Reset bit counter
                    }
                }
            }

            // If there are remaining bits that don’t fill a complete byte
            if (bitCount > 0) {
                bitBuffer <<= (8 - bitCount); // Left-shift remaining bits to fill the byte
                dos.write(bitBuffer); // Write the final partial byte
            }
        }
    }

    // Returns the size of a given file in bytes
    private long getFileSize(String filePath) {
        File file = new File(filePath);
        return file.length();
    }

    private void handleCompressButtonClick() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File inputFile = fileChooser.getSelectedFile();

            try {
                String inputFilePath = inputFile.getAbsolutePath();
                String outputFilePath = inputFile.getParent() + "/compressed.bin";

                // Step 1: Build frequency table
                buildFrequencyTable(inputFilePath);

                // Step 2: Build Huffman Tree
                Node root = buildHuffmanTree();
                if (root == null) {
                    outputTextArea.setText("The input file is empty. No compression needed.");
                    return; // Exit
                }

                // Step 3: Generate Huffman codes
                generateHuffmanCodes(root, "");

                // Step 4: Compress the file
                long startTime = System.nanoTime(); // Start timing compression
                compressFile(inputFilePath, outputFilePath);
                long endTime = System.nanoTime(); // End timing compression
                double compressionTimeInMillis = (endTime - startTime) / 1e6;

                // Time conversion into minutes, seconds, and milliseconds
                long totalTimeInMillis = (long) compressionTimeInMillis;
                long minutes = totalTimeInMillis / 60000;
                long seconds = (totalTimeInMillis % 60000) / 1000;
                long millis = totalTimeInMillis % 1000;

                // Step 5: Show file sizes and compression ratio
                long originalSize = getFileSize(inputFilePath);
                long compressedSize = getFileSize(outputFilePath);
                double compressionRatio = (double) (compressedSize) / originalSize * 100; // Percentage

                outputTextArea.setText(String.format("Original File Size: %d bytes\n", originalSize));
                outputTextArea.append(String.format("Compressed File Size: %d bytes\n", compressedSize));
                outputTextArea.append(String.format("Compression Ratio: %.2f%%\n", compressionRatio));
                outputTextArea.append(
                        String.format("Time Taken to Compress: %d min %d sec %d ms\n", minutes, seconds, millis));

            } catch (IOException ex) {
                outputTextArea.setText("Error: " + ex.getMessage());
            }
        }
    }

    private void handleDecompressButtonClick() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Compressed Files", "bin"));
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File inputFile = fileChooser.getSelectedFile();

            try {
                String compressedFilePath = inputFile.getAbsolutePath();
                String decompressedFilePath = inputFile.getParent() + "/decompressed.txt";

                // Step 1: Decompress the file
                long startTime = System.nanoTime(); // Start timing decompression
                decompressFile(compressedFilePath, decompressedFilePath);
                long endTime = System.nanoTime(); // End timing decompression
                double decompressionTimeInMillis = (endTime - startTime) / 1e6;

                // Time conversion into minutes, seconds, and milliseconds
                long totalTimeInMillis = (long) decompressionTimeInMillis;
                long minutes = totalTimeInMillis / 60000;
                long seconds = (totalTimeInMillis % 60000) / 1000;
                long millis = totalTimeInMillis % 1000;

                // Step 2: Show file sizes
                long compressedSize = getFileSize(compressedFilePath);
                long decompressedSize = getFileSize(decompressedFilePath);

                outputTextArea.setText(String.format("Compressed File Size: %d bytes\n", compressedSize));
                outputTextArea.append(String.format("Decompressed File Size: %d bytes\n", decompressedSize));
                outputTextArea.append(
                        String.format("Time Taken to Decompress: %d min %d sec %d ms\n", minutes, seconds, millis));

            } catch (IOException ex) {
                outputTextArea.setText("Error: " + ex.getMessage());
            }
        }
    }

    // Decompression logic (same as the provided code)
    private void decompressFile(String inputFilePath, String outputFilePath) throws IOException {
        // Decompression implementation (the existing code for decompression)
        // You can reuse the decompression logic as it is from your original code.
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(inputFilePath), 8192));
                BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {

            // Step 1: Read the number of unique characters
            int uniqueCharsCount = dis.readInt();

            // Step 2: Rebuild the frequency table and create the Huffman Tree
            int[] frequencyTable = new int[MAX_CHAR];
            for (int i = 0; i < uniqueCharsCount; i++) {
                char ch = dis.readChar();
                int freq = dis.readInt();
                frequencyTable[ch] = freq;
                count += freq;
            }

            // Step 3: Build Huffman Tree from the frequency table
            buildHuffmanTreeFromFrequencyTable(frequencyTable);

            // Step 4: Start decoding the file
            Node root = heap[0]; // The root of the Huffman Tree
            Node currentNode = root;
            int bitBuffer = 0;
            int bitCount = 0;
            int ch;
            int charswritten = 0;

            while ((ch = dis.read()) != -1) {
                bitBuffer = (bitBuffer << 8) | ch; // Add the next byte to the bitBuffer
                bitCount += 8;

                while (bitCount >= 1) {
                    int bit = (bitBuffer >> (bitCount - 1)) & 1;
                    currentNode = (bit == 0) ? currentNode.left : currentNode.right;
                    bitCount--;

                    // If leaf node is reached, output the character
                    if (currentNode.left == null && currentNode.right == null) {
                        writer.write(currentNode.data);
                        charswritten += 1;
                        currentNode = root; // Reset to root for the next character
                    }
                    if (charswritten == count) {
                        break;
                    }
                }
            }

            writer.flush(); // Make sure everything is written to the output file
        }
    }

    // Helper method to rebuild the Huffman tree from the frequency table
    // Rebuilds the Huffman tree using the frequency table (used during
    // decompression)
    private void buildHuffmanTreeFromFrequencyTable(int[] frequencyTable) {
        // Reset heap before building the tree
        heapSize = 0;

        // Insert all characters with non-zero frequency into the heap
        for (int i = 0; i < MAX_CHAR; i++) {
            if (frequencyTable[i] > 0) {
                insertHeap(new Node((char) i, frequencyTable[i])); // Create a leaf node for each character
            }
        }

        // Repeatedly combine the two nodes with lowest frequencies
        while (heapSize > 1) {
            Node node1 = extractMin(); // Get first minimum node
            Node node2 = extractMin(); // Get second minimum node

            // Create a new internal node with combined frequency
            Node newNode = new Node(node1.frequency + node2.frequency);
            newNode.left = node1;
            newNode.right = node2;

            insertHeap(newNode); // Insert the new node back into the heap
        }
        // At the end, the heap contains only one node, which is the root of the Huffman
        // tree
    }

    // Entry point of the program - launches the GUI in a thread-safe manner
    public static void main(String[] args) {
        // Ensures that GUI creation runs on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            new HuffmanCompressionDecompressionGUI().setVisible(true); // Create and show the GUI
        });
    }

}
