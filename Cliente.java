package msg;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class Cliente {
    
    private Socket Cliente;
    private String Nome;
    private ObjectInputStream Ois;
    private ObjectOutputStream Oos;
    private boolean Ok = false;

    public Cliente(Socket cliente, ArrayList<String> nomes){
        this.Cliente = cliente;
        try{
            this.Ois = new ObjectInputStream(cliente.getInputStream());
            this.Oos = new ObjectOutputStream(cliente.getOutputStream());
            try{
                this.Nome = (String) this.Ois.readObject();
                this.Oos.flush();
                boolean teste = true;
                while(teste){
                    teste = false;
                    for(String nome:nomes) {
                        if(nome.equalsIgnoreCase(this.Nome)){
                            this.Oos.writeObject("outro");
                            this.Oos.flush();
                            this.Nome = (String) this.Ois.readObject();
                            teste = true;
                            break;
                        }
                    }
                }
                this.Oos.writeObject("ok");
                this.Ok = true;
            }catch(ClassNotFoundException ex){
                System.out.println("Não foi possível obter o nome do cliente"+cliente.getInetAddress()+", fechando conexão!");
                ex.printStackTrace();
                System.out.println("--------------------------------------------------------------------------------");
                cliente.close();
            }
        }catch(IOException ex){
            System.out.println("Falha ao tentar abrir canal de comicação com o cliente "+cliente.getInetAddress()+", fechando conexão!");
            ex.printStackTrace();
            System.out.println("--------------------------------------------------------------------------------");
            try { cliente.close();} catch (IOException ex1) {}
        }
    }

    public String getNome() {
        return Nome;
    }
    public Socket getCliente() {
        return Cliente;
    }
    public ObjectInputStream getOis() {
        return Ois;
    }
    public ObjectOutputStream getOos() {
        return Oos;
    }
    public boolean isOk() {
        return Ok;
    }

    public void setOk(boolean Ok) {
        this.Ok = Ok;
    }
    
}
