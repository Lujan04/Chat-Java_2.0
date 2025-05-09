package Cliente;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Cliente {
    private static volatile boolean chatActivo = true;

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 1234);
            System.out.println("Conectado al servidor.");

            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);

            System.out.print("Introduce tu palabra clave de salida: ");
            String palabraClave = scanner.nextLine();
            salida.println(palabraClave);

            // Hilo de recepción
            Thread hiloRecepcion = new Thread(() -> {
                try {
                    String mensaje;
                    while (chatActivo && (mensaje = entrada.readLine()) != null) {
                        System.out.println("\nSERVIDOR: " + mensaje);
                        if (mensaje.contains("cerrado") || mensaje.contains("servidor")) {
                            chatActivo = false;
                            cerrarRecursos(socket, entrada, salida, scanner);
                        }
                        System.out.print("Escribe: ");
                    }
                } catch (IOException e) {
                    System.out.println("Error al recibir mensaje: " + e.getMessage());
                }
            });
            hiloRecepcion.start();

            // Envío de mensajes
            while (chatActivo) {
                System.out.print("Escribe: ");
                String mensaje = scanner.nextLine();
                salida.println(mensaje);

                if (mensaje.equals(palabraClave)) {
                    chatActivo = false;
                    cerrarRecursos(socket, entrada, salida, scanner);
                }
            }

            System.out.println("Chat finalizado.");
        } catch (IOException e) {
            System.out.println("Error al conectar: " + e.getMessage());
        }
    }

    private static void cerrarRecursos(Socket socket, BufferedReader entrada, PrintWriter salida, Scanner scan) {
        try {
            if (entrada != null) entrada.close();
            if (salida != null) salida.close();
            if (socket != null && !socket.isClosed()) socket.close();
            if (scan != null) scan.close();
        } catch (IOException e) {
            System.out.println("Error al cerrar recursos: " + e.getMessage());
        }
    }
}
