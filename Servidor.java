package msg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Servidor implements Acionador{
    
    private ServerSocket Servidor;
    private ArrayList<Cliente> Clientes;
    private ArrayList<Action> Actions;
    private Servidor This;

    public Servidor(int port) {
        try{
            this.Servidor = new ServerSocket(port);
        }catch(IOException ex){
            System.out.println("Falha ao abrir o servidor ("+port+"): ");
            ex.printStackTrace();
            System.out.println("Desligando aplicação...");
            System.exit(0);
        }
        System.out.println("Servidor criado com sucesso!");
        startServerAccept();
        this.Clientes = new ArrayList<>();
        startServerConsoleTrattment();
        This = this;
    }
    
    private void startServerConsoleTrattment(){
        new Thread(new Runnable(){
            
            @Override
            public void run(){
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                while(true){
                    try{
                        String str = br.readLine();
                        Actions.add(new Action(str, This));
                    }catch(IOException ex){
                        System.out.println("Erro ao tentar ler o console:");
                        ex.printStackTrace();
                        System.out.println("--------------------------------------------------------------------------------");
                    }
                }
            }
            
        }).start();
    }
    
    private void startServerAccept(){
        new Thread(new Runnable(){
            
            @Override
            public void run(){
                System.out.println("Esperando nova conexão...");
                Socket newClient;
                try{
                    newClient = Servidor.accept();
                    startClientTratment(newClient);
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
                Cliente cliente = new Cliente(client);
                if(cliente.isOk()){
                    Clientes.add(cliente);
                    try{
                        Object obj;
                        do{
                            obj = cliente.getOis().readObject();
                        }while(!(obj instanceof String));
                        Actions.add(new Action((String)obj, cliente));
                    }catch(IOException | ClassNotFoundException ex){
                        System.out.println("Exceção capturada ao tentar se cominucar com "+cliente.getNome()+", encerrando conexão!");
                        ex.printStackTrace();
                        System.out.println("--------------------------------------------------------------------------------");
                        cliente.setOk(false);
                        try{ cliente.getOos().writeObject("desconectado do servidor"); }catch(Exception ex1){}
                        try{ cliente.getOos().close(); }catch(Exception ex1){}
                        try{ cliente.getOis().close(); }catch(Exception ex1){}
                        try{ cliente.getCliente().close(); }catch(Exception ex1){}
                    }
                    Clientes.remove(cliente);
                }
            }
            
        }).start();
    }
    
    private void startActionsTratment(){
        new Thread(new Runnable(){
            
            @Override
            public void run(){
                while(0<Actions.size()){
                    Action act = Actions.get(0);
                    if(act.cliente instanceof Cliente){
                        try{
                            Cliente cliente = (Cliente)act.cliente;
                            if(act.action.equals("enviando mensagem")){
                                cliente.getOos().writeObject("envie a mensagem");
                            }
                        }catch(IOException ex){
                            System.out.println("Erro ao tratar o pedido");
                        }
                    } 
                    Actions.remove(0);
                }
            }
            
        }).start();
    }
    
    private class Action{
        
        public String action;
        public Acionador cliente;

        public Action(String action, Acionador cliente) {
            this.action = action;
            this.cliente = cliente;
        }
        
    }
    
}
