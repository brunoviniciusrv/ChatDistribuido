## Trabalho de sistemas distribuidos

## Participantes 
- Bruno Rodrigues 
- Eduardo Martins
- Lucas Tizzo


## Requisitos

 Java 17 ou superior (testado nas versões 17 e 25)

## Como Compilar

Abra o terminal na raiz do projeto e execute:

```sh
javac -d out $(find src -name "*.java")
```

 O comando compilará todos os arquivos para a pasta raiz/out

## Como Executar

1. **Inicie o servidor de descoberta:**
   ```sh
   java -cp out br.com.chat.distribuido.DiscoveryServer
   ```

2. **Inicie um cliente P2P:**
   ```sh
   java -cp out br.com.chat.distribuido.P2PClient <host> <portaServidor> <nomeUsuario> <portaP2P>
   ```
   Exemplo:
   ```sh
   java -cp out br.com.chat.distribuido.P2PClient localhost 1600 ana 9001
   ```

## Funcionalidades

- Registro de usuários e grupos.
- Criação e controle de grupos de chat.
- Comunicação direta entre clientes (P2P).
- Listagem de usuários e grupos online.
- Upload/envio de arquivos para outros usuarios.

## Observações

- Cada cliente deve utilizar uma porta P2P e um nome  distintos.
- Por padrão o servidor se inicia na porta 1600.
