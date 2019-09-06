package msg;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Cliente implements Acionador{
    
    private Socket Cliente;
    private String Nome;
    private ObjectInputStream Ois;
    private ObjectOutputStream Oos;
    private boolean Ok = false;

    public Cliente(Socket cliente){
        this.Cliente = cliente;
        try{
            this.Ois = new ObjectInputStream(cliente.getInputStream());
            this.Oos = new ObjectOutputStream(cliente.getOutputStream());
            try{
                this.Nome = (String) Ois.readObject();
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
