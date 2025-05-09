package servidor;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Servidor {
    private static volatile boolean servidorActivo = true;
    private static String palabraClaveServidor;
    private static final List<ClienteHandler> clientesConectados = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Uso: java Servidor <maxClientes> <palabraClaveServidor>");
            return;
        }

        int maxClientes = Integer.parseInt(args[0]);
        palabraClaveServidor = args[1];
        int puerto = 1234;

        ServerSocket serverSocket = new ServerSocket(puerto);
        System.out.println("Servidor iniciado en el puerto " + puerto + ". Esperando conexiones...");

        while (servidorActivo) {
            if (clientesConectados.size() < maxClientes) {
                Socket clienteSocket = serverSocket.accept();
                ClienteHandler handler = new ClienteHandler(clienteSocket);
                clientesConectados.add(handler);
                new Thread(handler).start();
                System.out.println("Cliente conectado. Total clientes: " + clientesConectados.size());
            }
        }

        serverSocket.close();
        System.out.println("Servidor cerrado.");
    }

    private static class ClienteHandler implements Runnable {
        private Socket socket;
        private BufferedReader entrada;
        private PrintWriter salida;
        private Scanner scanner = new Scanner(System.in);
        private boolean conectado = true;
        private String palabraClaveCliente;

        public ClienteHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                salida = new PrintWriter(socket.getOutputStream(), true);

                salida.println("Introduce tu palabra clave de salida:");
                palabraClaveCliente = entrada.readLine();

                Thread hiloLectura = new Thread(() -> {
                    try {
                        String mensaje;
                        while (conectado && (mensaje = entrada.readLine()) != null) {
                            System.out.println("\nCLIENTE: " + mensaje);

                            if (mensaje.equals(palabraClaveCliente)) {
                                salida.println("Chat cerrado por palabra clave propia.");
                                cerrarConexion();
                            } else if (mensaje.equals(palabraClaveServidor)) {
                                cerrarTodosLosClientes();
                                servidorActivo = false;
                            }

                            System.out.print("Escribe: ");
                        }
                    } catch (IOException e) {
                        System.out.println("Error leyendo del cliente.");
                        cerrarConexion();
                    }
                });
                hiloLectura.start();

                while (conectado && servidorActivo) {
                    System.out.print("Escribe: ");
                    String mensaje = scanner.nextLine();
                    salida.println(mensaje);

                    if (mensaje.equals(palabraClaveCliente)) {
                        salida.println("Servidor ha cerrado tu chat.");
                        cerrarConexion();
                    } else if (mensaje.equals(palabraClaveServidor)) {
                        salida.println("Servidor ha cerrado todos los chats.");
                        cerrarTodosLosClientes();
                        servidorActivo = false;
                    }
                }

            } catch (IOException e) {
                System.out.println("Error con el cliente.");
            }
        }

        private void cerrarConexion() {
            conectado = false;
            clientesConectados.remove(this);
            try {
                if (entrada != null) entrada.close();
                if (salida != null) salida.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                System.out.println("Error al cerrar recursos del cliente.");
            }
            if (clientesConectados.isEmpty()) {
                System.out.println("Sin clientes conectados. Cerrando servidor.");
                servidorActivo = false;
            }
        }

        private void cerrarTodosLosClientes() {
            synchronized (clientesConectados) {
                for (ClienteHandler c : clientesConectados) {
                    c.salida.println("El servidor se está cerrando.");
                    c.cerrarConexion();
                }
                clientesConectados.clear();
            }
        }
    }
}
