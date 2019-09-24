package msg;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Servidor{
    
    private ServerSocket Servidor;
    private ArrayList<Cliente> Clientes;
    private ArrayList<Action> Actions;
    private boolean readMsg = false;
    private String tempMsg = "";

    public Servidor(int port) {
        try{
            Servidor = new ServerSocket(port);
        }catch(IOException ex){
            System.out.println("Falha ao abrir o servidor ("+port+"): ");
            ex.printStackTrace();
            System.out.println("Desligando aplicação...");
            System.exit(0);
        }
        System.out.println("Servidor criado com sucesso!");
        Clientes = new ArrayList<>();
        Actions = new ArrayList<>();
        startServerAccept();
//        startActionsTratment();
    }
    
    private void startServerAccept(){
        new Thread(new Runnable(){
            
            @Override
            public void run(){
                Socket newClient;
                try{
                    while(true){
                        newClient = Servidor.accept();
                        startClientTratment(newClient);
                    }
                }catch(IOException ex){
                    System.out.println("Um usuário tentou se conectar e foi lançada a seguinte exceção:");
                    ex.printStackTrace();
                    System.out.println("--------------------------------------------------------------------------------");
                }
            }
        }).start();
    }
    
    private void startClientTratment(Socket client){
        new Thread(new Runnable(){
            
            @Override
            public void run(){
                ArrayList<String> nomes = new ArrayList<>();
                for(Cliente cliente:Clientes) nomes.add(cliente.getNome());
                Cliente cliente = new Cliente(client, nomes);
                if(cliente.isOk()){
                    try{
                        sendAll(new StringBuilder("* ").append(cliente.getNome()).append(" entrou no servidor! *").toString());
                        System.out.println("* "+cliente.getNome()+" entrou no servidor! *");
                        Clientes.add(cliente);
                        Object obj;
                        while(true){
                            do{
                                obj = cliente.getOis().readObject();
                            }while(!(obj instanceof String));
                            String str = (String) obj;
                            if(str.equals("enviando mensagem")){
                                StringBuilder msg = new StringBuilder("[").append(cliente.getNome()).append("]: ");
                                msg.append((String) cliente.getOis().readObject());
                                sendAll(msg.toString());
                                System.out.println(msg.toString());
                            }else if(str.equals("enviando arquivo")){
                                int port = -1;
                                StringBuilder path = new StringBuilder();
                                final StringBuilder MSG = new StringBuilder("* ").append(cliente.getNome()).append(" enviou o arquivo: ");
                                for(int i=1; i<101; i++){
                                    try{
                                        ServerSocket server = new ServerSocket(Servidor.getLocalPort()+i);
                                        port = server.getLocalPort();
                                        server.close();
                                        break;
                                    }catch(IOException ex){}
                                }
                                try{cliente.getOos().writeInt(port); cliente.getOos().flush(); }catch(Exception ex){}
                                if(port>0){
                                    final int port2 = port;
                                    new Thread(new Runnable(){
                                        
                                        final int Port = port2;
                                        final StringBuilder Path = path, MsG = MSG;
                                        
                                        @Override
                                        public void run(){
                                            ServerSocket server = null;
                                            try{
                                                server = new ServerSocket(Port);
                                                Socket client = server.accept();
                                                ObjectInputStream ois = new ObjectInputStream(client.getInputStream());
                                                path.append(recieveFile(ois));
                                                ois.close();
                                                client.close();
                                                server.close();
                                                MsG.append(new StringBuilder(path.reverse().toString().split("\\\\")[0]).reverse());
                                                path.reverse();
                                                System.out.println(MsG);
                                                sendAll(MsG.toString());
                                            }catch(IOException ex){}
                                            if(server != null)
                                            for(Cliente client:Clientes){
                                                try{
                                                    if(!client.getNome().equals(cliente.getNome())){
                                                        client.getOos().writeObject("enviando arquivo");
                                                        client.getOos().writeInt(Port);
                                                        client.getOos().flush();
                                                        Socket Client = server.accept();
                                                        ObjectOutputStream oos = new ObjectOutputStream(Client.getOutputStream());
                                                        File file = new File(path.toString());
                                                        SendFile(file, oos);
                                                        Client.close();
                                                    }
                                                }catch(Exception ex){}
                                            }
                                            try{server.close();}catch(Exception ex){}
                                        }
                                        
                                    }).start();
                                }
                            }else if(str.equals("saindo do servidor")){
                                try{ cliente.getOos().close(); }catch(IOException ex){}
                                try{ cliente.getOis().close(); }catch(IOException ex){}
                                try{ cliente.getCliente().close(); }catch(IOException ex){}
                                Clientes.remove(cliente);
                                String msg = new StringBuilder("* ").append(cliente.getNome()).append(" saiu do servidor! *").toString();
                                sendAll(msg);
                                System.out.println(msg);
                                break;
                            }
                        }
                    }catch(IOException | ClassNotFoundException ex){
                        System.out.println("Exceção capturada ao tentar se cominucar com "+cliente.getNome()+", encerrando conexão!");
                        ex.printStackTrace();
                        System.out.println("--------------------------------------------------------------------------------");
                        cliente.setOk(false);
                        try{ cliente.getOos().writeObject("desconectado do servidor"); }catch(Exception ex1){}
                        try{ cliente.getOos().close(); }catch(Exception ex1){}
                        try{ cliente.getOis().close(); }catch(Exception ex1){}
                        try{ cliente.getCliente().close(); }catch(Exception ex1){}
                        Clientes.remove(cliente);
                    }
                }
            }
            
        }).start();
    }
    
    private void sendAll(String msg) throws IOException{
        for(Cliente client:Clientes){
            client.getOos().writeObject("enviando mensagem");
            client.getOos().writeObject(msg.toString());
        }
    }
    
    public static String recieveFile(ObjectInputStream ois){
        StringBuilder path = new StringBuilder();
        try{
            path.append(new File(".").getCanonicalPath()).append("\\files\\").append( (String) ois.readObject());
            File file = new File(path.toString());
            for(int i=1; file.exists(); i++){
                String ext = new StringBuilder(path.reverse().toString().split("\\\\")[0]).reverse().toString().split("\\.")[1];
                path.delete(0, ext.length()+1);
                if(i==1) path.reverse().append(" (").append(i).append(')');
                else path.replace(0, (i/10)+2, ")"+i).reverse();
                path.append('.').append(ext);
                file = new File(path.toString());
            }
            file.getParentFile().mkdirs();
            file.createNewFile();
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            int count;
            byte[] buffer = new byte[2048];
            while((count = ois.read(buffer))>0) {
                bos.write(buffer, 0, count);
            }
            bos.flush();
            bos.close();
        }catch(IOException | ClassNotFoundException ex){ex.printStackTrace();}
        return path.toString();
    }
    
    public static void SendFile(File file, ObjectOutputStream oos) throws IOException{
//        oos.writeObject(file.getCanonicalPath().replace(new File(".").getCanonicalPath(), ""));
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        int count;
        byte[] buffer = new byte[2048];
        while((count = bis.read(buffer)) > 0){
            oos.write(buffer, 0, count);
        }
        bis.close();
        oos.close();
    }
    
    private class Action{
        
        public String action;
        public Cliente cliente;

        public Action(String action, Cliente cliente) {
            this.action = action;
            this.cliente = cliente;
        }
        
    }
    
}
