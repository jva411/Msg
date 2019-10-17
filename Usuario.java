package msg;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Usuario {
    
    private Socket User;
    private String Nome;
    private ObjectOutputStream Oos;
    private ObjectInputStream Ois;
    private BufferedReader Br;
    private boolean OkMsg = false, OkFile = false;
    
    public Usuario(String ip, int porta){
        try{
            User = new Socket(ip, porta);
            Oos = new ObjectOutputStream(User.getOutputStream());
            Ois = new ObjectInputStream(User.getInputStream());
            System.out.println("Conectado ao servidor com sucesso!");
            Br = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Digite o seu nome:");
            Nome = Br.readLine();
            Oos.writeObject(Nome);
            while(((String)Ois.readObject()).equals("outro")){
                System.out.println("Nome já usado, digite outro:");
                Nome = Br.readLine();
                Oos.writeObject(Nome);
            }
            System.out.println("Bem vindo ao servidor "+Nome);
            startUserTratment();
            startServerTratment();
        }catch(IOException | ClassNotFoundException ex){
            System.out.println("Não foi possível estabelecer uma conexão com o servidor!");
        }
    }
    
    public void startUserTratment(){
        new Thread(new Runnable(){
            
            @Override
            public void run(){
                while(true){
                    try{
                        String str = Br.readLine();
                        if(str.startsWith("/")){
                            String[] args = str.split(" ");
                            String a0 = args[0].toLowerCase().split("/")[1];
                            if(a0.equals("sair")){
                                try{
                                    Oos.writeObject("saindo do servidor");
                                    Oos.close();
                                    Ois.close();
                                    User.close();
                                    System.out.println("* Você saiu do servidor *");
                                    System.exit(0);
                                }catch(Exception ex){
                                    try{ Oos.close(); }catch(Exception ex2){}
                                    try{ Ois.close(); }catch(Exception ex2){}
                                    try{ User.close(); }catch(Exception ex2){}
                                    System.out.println("* Você saiu do servidor *");
                                    System.exit(0);
                                }
                            }else if(a0.equals("file")){
                                if(args.length>1){
                                    StringBuilder path = new StringBuilder(new File(".").getCanonicalPath()), path2 = new StringBuilder();
                                    path2.append(args[1]);
                                    for(int i=2; i<args.length; i++) path2.append(' ').append(args[i]);
                                    path.append('\\').append(path2);
                                    File file = new File(path.toString());
                                    if(file.exists()){
                                        Oos.writeObject("enviando arquivo");
                                        new Thread(new Runnable(){
                                            
                                            @Override
                                            public void run(){
                                                try{
                                                    int port = -1;
                                                    try{
                                                        port = Ois.readInt();
                                                    }catch(Exception ex){
                                                        ex.printStackTrace();
                                                    }
                                                    if(port != -1){
                                                        Socket user = new Socket(User.getInetAddress().getHostAddress(), port);
                                                        try{
                                                            ObjectOutputStream oos = new ObjectOutputStream(user.getOutputStream());
                                                            StringBuilder sb = new StringBuilder(path2.reverse().toString().split("\\\\")[0]);
                                                            sb.append(new StringBuilder(Nome).append('_').reverse());
                                                            path2.delete(0, sb.length()).append(sb).reverse();
                                                            oos.writeObject(path2.toString().replaceAll("\\.+\\\\", ""));
                                                            Servidor.SendFile(file, oos);
                                                            user.close();
                                                        }catch(Exception ex){
                                                            ex.printStackTrace();
                                                        }
                                                    }else System.out.println("* Erro ao tentar enviar arquivo! *");
                                                }catch(IOException ex){}
                                            }
                                            
                                        }).start();
                                    }else System.out.println("O arquivo \""+path+"\" não foi encontrado ou não existe!");
                                }else System.out.println("/file <arquivo>\t- para enviar um arquivo");
                            }else{
                                System.out.println("/sair\t\t- para sair do servidor");
                                System.out.println("/file <arquivo>\t- para enviar um arquivo");
                            }
                        }else if(str.length()>0){
                            Oos.writeObject("enviando mensagem");
                            Oos.writeObject(str);
                        }
                    }catch(IOException ex){
                        System.out.println("Erro ao tentar ler o console:");
                        ex.printStackTrace();
                        System.out.println("--------------------------------------------------------------------------------");
                    }
                }
            }
            
        }).start();
    }
    
    public void startServerTratment(){
        new Thread(new Runnable(){
            
            @Override
            public void run(){
                try{
                    while(true){
                        Object obj;
                        do{
                            obj = Ois.readObject();
                        }while(!(obj instanceof String));
                        String str = (String)obj;
                        if(str.equals("enviando mensagem")){
                            StringBuilder sb = new StringBuilder();
                            sb.append(Ois.readObject());
                            System.out.println(sb);
                        }else if(str.equals("enviando arquivo")){
                            for(int i=1; i<101; i++){
                                try{
                                    Socket user = new Socket(User.getInetAddress(), User.getPort()+i);
                                    try{
                                        ObjectInputStream ois = new ObjectInputStream(user.getInputStream());
                                        StringBuilder path = new StringBuilder(new File(".").getCanonicalPath()).append((String)ois.readObject());
                                        File file = new File(path.toString());
                                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                                        int count;
                                        byte[] buffer = new byte[2048];
                                        while((count = ois.read(buffer)) > 0){
                                            bos.write(buffer, 0, count);
                                        }
                                        bos.flush();
                                        bos.close();
                                        ois.close();
                                        user.close();
                                    }catch(Exception ex){}
                                    break;
                                }catch(Exception ex){}
                            }
                        }
                    }
                }catch(Exception ex){
                    
                }
            }
            
        }).start();
    }
    
}
