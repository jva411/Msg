import java.io.*;
import java.net.*;
import java.util.*;

public class Msg {

    public static void main(String[] args){

        try{
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String r;
            do{
                System.out.println("Digite \'server\' para iniciar um servidor ou \'user\' para se conectar à um: ");
                r = br.readLine().toLowerCase();
            }while(!(r.equals("server") || r.equals("user")));
            if(r.equals("server")){
                int port = -1;
                do{
                    System.out.print("Digite a porta: ");
                    try{
                        port = Integer.parseInt(br.readLine());
                        if(port<0) System.out.println("Você deve digitar um número positivo inteiro!");
                    }catch(Exception ex){
                        System.out.println("Você deve digitar um número positivo inteiro!");
                    }
                }while(port<=0);
                startServer(port);
                startUser("localhost", 5534);
            }
            br.close();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }

    }

    public static ServerSocket Server;

    public static  void startServer(int port){

        Thread t = new Thread(new Runnable(){

            @Override
            public void run(){
                try{
                    Server = new ServerSocket(port);
                    while(true) tratamentoDeClient(Server.accept());
                }catch(Exception ex){
                    ex.printStackTrace();
                }
            }

        });
        t.start();
        Thread t2 = new Thread(new Runnable(){

            @Override
            public void run(){

            }

        });
        t2.start();

    }

    public static void tratamentoDeClient(Socket client){
        Thread t = new Thread(new Runnable(){

            @Override
            public void run(){
                try{
                    ObjectInputStream ois = new ObjectInputStream(client.getInputStream());
                    ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
                    Cliente Client = new Cliente(ois.readObject()+"", client);
                    Clients.add(Client);
                    System.out.println("Novo usuário conectado: "+client.getInetAddress()+" ["+Client.Name+"]");
                    while(true){
                        Object obj = null;
                        do{
                            obj = ois.readObject();
                        }while(!(obj instanceof String));
                        String str = (String)obj;
                        if(str.equals("enviando mensagem")){
                            print();
                        }
                    }
                }catch(Exception ex){
                    ex.printStackTrace();
                }
            }

        });
        t.start();
    }

    public static ArrayList<Cliente> Clients = new ArrayList<>();

    public static class Cliente{
        public String Name;
        public Socket Socket;

        public Cliente(String name, Socket socket){
            Name = name;
            Socket = socket;
        }
    }

    public static void startUser(String ip, int port){
        Thread t = new Thread(new Runnable(){

            @Override
            public void run(){
                try{
                    Socket user = new Socket(ip, port);
                    ObjectOutputStream oos = new ObjectOutputStream(user.getOutputStream());
                }catch(Exception ex){
                    ex.printStackTrace();
                }
            }

        });
        t.start();
    }

    public static void print(String what, boolean rightSide){
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder sb = new StringBuilder(what);
        int i = 0;
        for(i=0;i<(what.length()/25)-1;i++) lines.add(sb.substring(i, i+25));
        lines.add(sb.substring(i, what.length()));
        if(rightSide){
            for(String line:lines) System.out.println("                         "+line);
        }else{
            for(String line:lines) System.out.println(line);
        }
    }

}