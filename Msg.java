package msg;

import java.util.Scanner;

public class Msg {

    public static String CODIGO = "K12LHMANQ fim da mensagem!";

    
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        String escolha;
        do{
        System.out.println("Digite 'servidor' para iniciar um servidor ou 'usuario' para se conectar à um: ");
        escolha = scan.nextLine().toLowerCase();
        }while(!(escolha.equals("servidor") || escolha.equals("usuario")));
        if(escolha.equals("servidor")){
            System.out.println("Digite a porta: ");
            new Servidor(scan.nextInt());
        }else{
            System.out.println("Digite o ip: (Ex: 127.0.0.1:3354)");
            escolha = scan.nextLine();
            while(!escolha.matches("^(\\d{1,3}\\.){3}\\d{1,3}:\\d{2,6}$")){
                System.out.println("Ip inválido! Digite outro:");
                escolha = scan.nextLine();
            }
            new Usuario(escolha.split(":")[0], Integer.parseInt(escolha.split(":")[1]));
        }
    }
    
}
