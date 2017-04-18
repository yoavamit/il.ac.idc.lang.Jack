package il.ac.idc.lang.emulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class VMEmulatorCmdDebugger {

	private static boolean vmStarted = false;
	
	public static void main(String[] args) throws IOException, InterruptedException {
		Socket requestSocket;
		PrintWriter requestPrinter;
		BufferedReader requestReader;
		
		String[] vmArgs = new String[6];
		vmArgs[0] = args[0];
		vmArgs[1] = "--debug";
		vmArgs[2] = "--eventPort";
		vmArgs[3] = "1234";
		vmArgs[4] = "--requestPort";
		vmArgs[5] = "8080";
		Thread vm = runVM(vmArgs);
		
		Thread eventListener = eventListener();
		
		while (!vmStarted) {
			Thread.sleep(100);
		}
		requestSocket = new Socket("127.0.0.1", 8080);
		requestReader = new BufferedReader(new InputStreamReader(requestSocket.getInputStream()));
		requestPrinter = new PrintWriter(requestSocket.getOutputStream());
		Scanner input = new Scanner(System.in);
		String command = null;
		do {
			System.out.print("> ");
			command = input.nextLine();
			if (command.equals("help")) {
				System.out.println("available commands:\n"
						+ "set|N\t\t\tSet a breakpoint at line N\n"
						+ "clear|N\t\t\tClear breakpoint at line N\n"
						+ "data|N|T\t\t\tReturns the contents of the VM heap at address N for object type T\n"
						+ "exit\t\t\tTerminated the VM\n"
						+ "resume\t\t\tResume normal program execution\n"
						+ "stack\t\t\tReturns the contents of the VM stack\n"
						+ "step\t\t\tExecute a single VM command and suspend execution immediately after\n"
						+ "suspend\t\t\tSuspend the VM\n"
						+ "value-get|F|VAR\t\tReturns the value of variable VAR from frame ID F\n"
						+ "vars|F\t\t\tReturns the variable names from frame ID F\n"
						+ "value-set|F|VAR|VALSets the variable VAR from frame ID F to value VAL");
			} else {
				requestPrinter.println(command);
				requestPrinter.flush();
				if (!command.equals("exit")) {
					String output = requestReader.readLine();
					System.out.println(output);
				}
			}
		} while (command != null && vmStarted);
		requestSocket.close();
		input.close();
		vm.join();
		eventListener.join();
	}
	
	public static Thread eventListener() {
		Runnable r = new Runnable() {
			
			@Override
			public void run() {
				boolean isListening = true;
				while (isListening) {
					try {
						Socket listener = new Socket("127.0.0.1", 1234);
						BufferedReader eventReader = new BufferedReader(new InputStreamReader(listener.getInputStream()));
						String event = eventReader.readLine();
						if (event != null) {
							if (event.equals("terminated")) {
								isListening = false;
								vmStarted = false;
							} else if (event.equals("started")) {
								vmStarted = true;
							}
							System.out.println("VM event: " + event);
						}
					
					listener.close();
					} catch (IOException e) {
					}
				}
			}
		};
		Thread t = new Thread(r);
		t.setName("Event listener");
		t.start();
		return t;
	}
	
	public static Thread runVM(final String[] args) {
		Runnable r = new Runnable() {
			public void run() {
				VMEmulator.main(args);
			}
		};
		Thread t = new Thread(r);
		t.setName("Jack VM");
		t.start();
		return t;
	}
}
