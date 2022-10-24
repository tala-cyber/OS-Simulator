import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IllegalFormatPrecisionException;
import java.util.Random;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;




public class OS {
    static int ID=0; 
    static ArrayList<Process> Processes = new ArrayList<Process>();
    static ArrayList<Process> Ready = new ArrayList<Process>();
    static ArrayList<Process> IO = new ArrayList<Process>();
    static ArrayList<PCB> Memory=new ArrayList<PCB>();
    static int stats; static int lock;

    static class Operation{
        public String type; public int cycles;
        public Operation(String t, int c){type=t; cycles=c;}
    }

    static class Process{
        public ArrayList<Operation> ops; public String state; public int template; public int PID;
        public Process(ArrayList<Operation> operations, int t){ops=operations; state="READY"; template =t;PID=ID++;}
    }

    static class PCB{
        public int PID; public ArrayList<Operation> ops; public String state; public static int curr_op;
        public PCB(Process p){PID=p.PID; ops=p.ops; state=p.state; curr_op=0;}
    }

    public static int searchMemory(Process p){
        for(int i=0; i<Memory.size(); i++){
            if(Memory.get(i).PID==p.PID){
                return i;
            }
        }
        Memory.add(new PCB(p));
        return Memory.size()-1;
    }

    public static Process createProcess(String template) throws SAXException, IOException, ParserConfigurationException{
        // Get file and parse
        File xml = new File(template);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = (Document) builder.parse(xml);
        doc.getDocumentElement().normalize();

        //Prepare list to iterate
        NodeList op_list = doc.getElementsByTagName("operation");

        //Prepare list to return
        ArrayList<Operation> operations = new ArrayList<Operation>();

        //Iterate over the list of operations
        for(int i=0; i<op_list.getLength(); i++){
            Node op  = op_list.item(i);
            if(op.getNodeType() == Node.ELEMENT_NODE){
                Element operation = (Element) op;

                String operationType = operation.getElementsByTagName("type").item(0).getTextContent();
                String min = operation.getElementsByTagName("min").item(0).getTextContent();
                String max = operation.getElementsByTagName("max").item(0).getTextContent();

                int int_min = Integer.valueOf(min);
                int int_max = Integer.valueOf(max);

                Random rand = new Random();
                int cycles = rand.nextInt(int_max-int_min);

                Operation opp = new Operation(operationType, cycles+int_min);
                operations.add(opp);
            }
        }
        if(template=="pf1.xml"){
            return new Process(operations, 1);
        }else if(template=="pf2.xml"){
            return new Process(operations, 2);
        }
        return new Process(operations, 3);
    }

    public static void createProcesses(String template, int n) throws SAXException, IOException, ParserConfigurationException{
        for(int i=0; i<n; i++){
            Processes.add(createProcess(template));
        }        
    }

    public static synchronized void CPU(Process p,int quantum) throws InterruptedException{
        Acquire();
        PCB block = Memory.get(searchMemory(p));
        Operation op=block.ops.get(block.curr_op);
        // each cycle is 10 milliseconds and the time quantum is 2 cycles
        if(quantum==0 || op.cycles<quantum){
            for(int i=0; i<op.cycles; i++){
                //ob.wait(10);
            }
            op.cycles=0;
        }else{
            for(int i=0; i<quantum; i++){
                //ob.wait(10);
            }
            op.cycles=op.cycles-quantum;
        }
        Release();
    }

    public static void Acquire(){
        lock++;
    }
    public static void Release(){
        lock--;
    }

    public static Process Fork(Process p) throws SAXException, IOException, ParserConfigurationException{
        PCB block = Memory.get(searchMemory(p));
        Operation op=block.ops.get(block.curr_op);
        op.cycles=0;
        if(p.template==1){
            return createProcess("pf1.xml");
        }else if(p.template==2){
            return createProcess("pf2.xml");
        }
        return createProcess("pf3.xml");
    }

    public static void IO(Operation op, int quantum) throws InterruptedException{
        Object ob = new Object();

        // each cycle is 10 milliseconds
        if(quantum==0 || op.cycles<quantum){
            for(int i=0; i<op.cycles; i++){
                //ob.wait(10);
            }
            op.cycles=0;
        }else{
            for(int i=0; i<quantum; i++){
                //ob.wait(10);
            }
            op.cycles=op.cycles-quantum;
        }
    }

    public static void RoundRobin() throws InterruptedException, SAXException, IOException, ParserConfigurationException{
        
        Process p; Operation op;
        while(!Processes.isEmpty()){
            if(!Ready.isEmpty()){
                p=Ready.get(0);
                PCB block = Memory.get(searchMemory(p));
                op=block.ops.get(block.curr_op);
                CPU(p, 2);
                ClassifyProcess(p);
                updateIO();
                printStatsRR();
            }else if(IO.isEmpty()){
                break;
            }else{
                updateIO();
                printStatsRR();
            }
        }
        
    }

    public static void updateIO() throws InterruptedException, SAXException, IOException, ParserConfigurationException{
        for(int i=0; i<IO.size(); i++){
            Process p =IO.get(i);
            PCB block = Memory.get(searchMemory(p));
            Operation op=block.ops.get(block.curr_op);
            op.cycles=op.cycles-2;
        }
        ArrayList<Process> copyIO = new ArrayList<>(IO);
        int size=copyIO.size();
        for(int i=0; i<size; i++){
            Process p =copyIO.get(i);
            ClassifyProcess(p);
        }
    }

    public static void ClassifyProcess(Process p) throws InterruptedException, SAXException, IOException, ParserConfigurationException{
        PCB block = Memory.get(searchMemory(p));
        Operation op=block.ops.get(block.curr_op);
        IO.remove(p); Ready.remove(p);
        if(op.cycles>0){
            if(op.type.equals("calculate")){
                Ready.add(p);
            }else if(op.type.equals("io")){
                IO.add(p);
            }
        }else{
            if(block.curr_op==4){
                Processes.remove(p); 
            }else{
                block.curr_op++;
                System.out.println("increment"); //System.exit(1);
                op=block.ops.get(block.curr_op);
                if(op.type.equals("calculate")){
                    Ready.add(p);
                }else if(op.type.equals("io")){
                    IO.add(p);
                }else{
                    Fork(p);
                    ClassifyProcess(p);
                }
            }
        }
    }


    public static void Organize(){
        for(int i=0; i<Processes.size(); i++){
            Process p = Processes.get(i);
            Operation op=p.ops.get(0);
            if(op.type.equals("calculate")){
                Ready.add(p);
            }else if(op.type.equals("io")){
                IO.add(p);
            }
        }
    }

    public static void printStatsRR(){
        if(stats==0){
            //do nothing
        }else{
            System.out.println("Ready: ");
            for(int i=0; i<Ready.size(); i++){
                System.out.print(Ready.get(i).PID);
                System.out.print("("+Memory.get(searchMemory(Ready.get(i))).curr_op+")--");
            }
            System.out.println();
            System.out.println("Waiting:   ");
            for(int i=0; i<IO.size(); i++){
                System.out.print(IO.get(i).PID);
                System.out.print("("+Memory.get(searchMemory(IO.get(i))).curr_op+")--");
            }
            System.out.println();
            if(Ready.size()>=1){
                System.out.println("Running:  "+Ready.get(0).PID);
            }else{
                System.out.println("Running:  ");
            }
            System.out.println(); System.out.println();

        }
    }

    public static void FCFS() throws InterruptedException, SAXException, IOException, ParserConfigurationException{
        for(int i=0; i<Processes.size(); i++){
            Process p = Processes.get(i);
            for(int j=0; j<5; j++){
                Operation op = p.ops.get(j);
                if(op.type.equals("calculate")){
                    CPU(p, 0);
                }else if(op.type.equals("io")){
                    IO(op, 0);
                }else{
                    Fork(p);
                }
            }
            printStatsFCFS(i);
        }
    }

    public static void printStatsFCFS(int i){
        if(stats==0){
            //do nothing
        }else{
            System.out.println("Just fnished executing: Process "+i);
        }
    }

    public static int customRR(int n1, int n2, int n3) throws InterruptedException, SAXException, IOException,ParserConfigurationException{
        createProcesses("pf1.xml", n1);
        createProcesses("pf2.xml", n2);
        createProcesses("pf3.xml", n3);
        System.out.println("Executing RR...");
        Organize();
        RoundRobin();
        return 1;  
    }
    public static int customFCFS(int n1, int n2, int n3) throws SAXException, IOException,ParserConfigurationException,InterruptedException{
        createProcesses("pf1.xml", n1);
        createProcesses("pf2.xml", n2);
        createProcesses("pf3.xml", n3);
        System.out.println("Executing FCFS...");
        FCFS();
        return 1;
    }



    public static void main(String args[]) throws SAXException, IOException, ParserConfigurationException, InterruptedException{
        System.out.println("Type the number of processes you wish to generate.");
        System.out.println("Program File 1: ");
        Scanner myObj = new Scanner(System.in);
        String pf1 = myObj.nextLine();
        System.out.println("Program File 2: ");
        String pf2 = myObj.nextLine();
        System.out.println("Program File 3: ");
        String pf3 = myObj.nextLine();
        System.out.println("Type RR for Round Robin or FCFS for First Come First Serve:");
        String algo = myObj.nextLine();
        System.out.println("Stats? Type 1 for yes or 0 for no.");
        stats = Integer.parseInt(myObj.nextLine());
        OS OS_Instance =  new OS();
        if(algo.equals("RR")){
            int code = customRR(Integer.parseInt(pf1), Integer.parseInt(pf2), Integer.parseInt(pf3));
        }else if(algo.equals("FCFS")){
            int code = customFCFS(Integer.parseInt(pf1), Integer.parseInt(pf2), Integer.parseInt(pf3));
        }else{
            System.out.println("Try Again");
        }

        System.out.println("Type x to exit");
        String exit = myObj.nextLine();
        if(exit.equals("x")){

        }          
        

    }
}


