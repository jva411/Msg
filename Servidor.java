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
        startActionsTratment();
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
                        for(Cliente client:Clientes) {
                            client.getOos().writeObject("enviando mensagem");
                            client.getOos().writeObject("* "+cliente.getNome()+" entrou no servidor! *");
                            client.getOos().writeObject(Msg.CODIGO);
                        }
                        System.out.println("* "+cliente.getNome()+" entrou no servidor! *");
                        Clientes.add(cliente);
                        Object obj;
                        while(true){
                            do{
                                obj = cliente.getOis().readObject();
                            }while(!(obj instanceof String));
                            boolean b = readMsg;
                            if(readMsg) {
                                tempMsg = (String)obj;
                                String s = (String) tempMsg;
                                while(readMsg) if(tempMsg.length()==0) readMsg = false;
                            }else {
                                Actions.add(new Action((String)obj, cliente));
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
    
    private void startActionsTratment(){
        new Thread(new Runnable(){
            
            @Override
            public void run(){
                while(true){
                    int i = Actions.size();
                    while(Actions.size() > 0){
                        Action act = Actions.get(0);
                            try{
                                final Cliente cliente = (Cliente)act.cliente;
                                if(act.action.equals("saindo do servidor")){
                                    cliente.getOos().writeObject("tchau");
                                    Clientes.remove(cliente);
                                    for(Cliente client:Clientes){
                                        client.getOos().writeObject("enviando mensagem");
                                        client.getOos().writeObject("* "+cliente.getNome()+" saiu do servidor *");
                                        client.getOos().writeObject(Msg.CODIGO);
                                    }
                                    try{ cliente.getOos().close(); }catch(Exception ex){}
                                    try{ cliente.getOis().close(); }catch(Exception ex){}
                                    try{ cliente.getCliente().close(); }catch(Exception ex){}
                                }else if(act.action.equals("enviando mensagem")){
                                    StringBuilder msg = new StringBuilder("[").append(cliente.getNome()).append("]: ");
                                    readMsg = true;
                                    cliente.getOos().writeObject("envie a mensagem");
                                    while(tempMsg.length()==0);
                                    String temp = tempMsg;
                                    while(!temp.equals(Msg.CODIGO)) {
                                        msg.append(temp);
                                        temp = (String)cliente.getOis().readObject();
                                    }
                                    for(Cliente client:Clientes){
                                        client.getOos().writeObject("enviando mensagem");
                                        client.getOos().writeObject(msg.toString());
                                        client.getOos().writeObject(Msg.CODIGO);
                                    }
                                    tempMsg = "";
                                    readMsg = false;
                                }else if(act.action.equals("enviando arquivo")){
                                    new Thread(new Runnable(){
                                        final Cliente Cliente = cliente;
                                        @Override
                                        public void run(){
                                            StringBuilder path = new StringBuilder();
                                            for(int i=1; i<101; i++){
                                                try{
                                                    ServerSocket server = new ServerSocket(Servidor.getLocalPort()+i);
                                                    try{Cliente.getOos().writeObject("envie o arquivo");
                                                        Socket client = server.accept();
                                                        ObjectInputStream ois = new ObjectInputStream(client.getInputStream());
                                                        path.append(recieveFile(ois));
                                                        ois.close();
                                                        client.close();
                                                        server.close();
                                                    }catch(Exception ex){}
                                                    break;
                                                }catch(Exception ex){}
                                            }
                                            if(path.length()>0){
                                                for(Cliente cliente:Clientes){
                                                    if(!cliente.getNome().equals(Cliente.getNome())){
                                                        for(int i=1; i<101; i++){
                                                            try{
                                                                ServerSocket server = new ServerSocket(Servidor.getLocalPort()+i);
                                                                try{
                                                                    cliente.getOos().writeObject("enviando arquivo");
                                                                    Socket client = server.accept();
                                                                    ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
                                                                    File file = new File(path.toString());
                                                                    SendFile(file, oos);
                                                                    client.close();
                                                                    server.close();
                                                                }catch(Exception ex){}
                                                                break;
                                                            }catch(Exception ex){}
                                                        }
                                                    }
                                                    try{
                                                        cliente.getOos().writeObject("enviando mensagem");
                                                        cliente.getOos().writeObject("["+Cliente.getNome()+"]: enviou o arquivo "+new StringBuilder(path.reverse().toString().split("\\\\")[0]).reverse());
                                                        cliente.getOos().writeObject(Msg.CODIGO);
                                                    }catch(Exception ex){}
                                                }
                                            }
                                        }
                                    }).start();
                                }
                            }catch(IOException | ClassNotFoundException ex){
                                Cliente client = (Cliente) act.cliente; 
                                System.out.println("Erro ao tratar o pedido do cliente: "+client.getNome());
                                System.out.println("Ip: "+client.getCliente().getInetAddress());
                                ex.printStackTrace();
                                System.out.println("--------------------------------------------------------------------------------");
                            }
                            Actions.remove(0);
                        }
                    try{ Thread.sleep(500);}catch(Exception ex){}
                    }
                }            
        }).start();
    }
    
    public static String recieveFile(ObjectInputStream ois){
        StringBuilder path = new StringBuilder();
        try{
            path.append(new File(".").getCanonicalPath()).append("\\files\\").append( (String) ois.readObject());
            File file = new File(path.toString());
            for(int i=1; file.exists(); i++){
                if(i==1) path.append("_(").append(i).append(')');
                else path.reverse().replace(0, (i/10)+1, "("+i).reverse();
                file = new File(path.toString());
            }
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            int count;
            byte[] buffer = new byte[2048];
            while((count = ois.read(buffer))>0) bos.write(buffer, 0, count);
            bos.flush();
            bos.close();
        }catch(IOException | ClassNotFoundException ex){}
        return path.toString();
    }
    
    public static void SendFile(File file, ObjectOutputStream oos) throws IOException{
        oos.writeObject(file.getCanonicalPath().replace(new File(".").getCanonicalPath(), ""));
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        int count;
        byte[] buffer = new byte[2048];
        while((count = bis.read(buffer)) > 0){
            oos.write(buffer, 0, count);
        }
        bis.close();
        oos.flush();
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
