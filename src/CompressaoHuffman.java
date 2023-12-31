import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class CompressaoHuffman {
    HashMap<Character, Integer> frequencia = new HashMap<>(); //Será utilizado para medir a frequencia de todas as letras e numeros
    HashMap<Character, String> codificacoes = new HashMap<>(); //Será utilizado para armazenar as codificações dos simbolos
    StringBuilder codificado = new StringBuilder(); //Utilizado para escrever os códigos de huffman no novo arquivo

    driverNode pilotos = new driverNode();
    String dbPath = "src/data/driversDB.db";
    RandomAccessFile arquivo;
    NoDeHuffman raiz;

    /**
     * Método que fará a compressão do arquivo realizando a chamada de várias outras funções
     */
    void comprimir(){
        contagemDeSimbolos(); //Primeiro será contabilizado todos os simbolos do arquivo e construido uma arvore binaria de huffman
        codificacaoDeHuffman(raiz, "", codificacoes); //Gerando códigos para as palavras

        try{
            ArrayList<Byte> ba = new ArrayList<>();
            arquivo = new RandomAccessFile(dbPath, "rw"); //Abrindo arquivo para criar o Byte Array comprimido
            arquivo.seek(0); //Apontado para o inicio do arquivo
            Integer IDloop = 1;

            Integer metadados = arquivo.readInt(); //Lendo todo o metadado

            String simbolo = metadados.toString(); //Convertendo o int para String
            codificarTexto(simbolo, codificacoes); //Enviando a String para ser codificada

            while (IDloop <= metadados){
                try{
                    if(arquivo.readChar() != '*'){ //Verificando se será um registro morto. Caso seja, não iremos comprimir pois será espaço inutilizado
                        Integer tamanhoRegistro = arquivo.readInt(); //Guardando tamanho do registro
                        codificarTexto(tamanhoRegistro.toString(), codificacoes);
                        byte[] bytes = new byte[tamanhoRegistro]; //Criando byte array do tamanho do registro
                        arquivo.readFully(bytes); //Lendo todo o registro de acordo com a quantidade de bytes
                        pilotos.fromByteArray(bytes); //Extrai o objeto do vetor de btyes

                        //Codificando toda a classe piloto
                        Integer ID = pilotos.getID(); //Recebendo ID para enviar a conversão de String
                        codificarTexto(ID.toString(), codificacoes);
                        codificarTexto(pilotos.getReference(), codificacoes);
                        codificarTexto(pilotos.getName(), codificacoes);
                        codificarTexto(pilotos.getSurname(), codificacoes);
                        codificarTexto(pilotos.getNatiotanlity(), codificacoes);
                        codificarTexto(pilotos.getDriverNumber(), codificacoes);
                        codificarTexto(pilotos.getDate().toString(), codificacoes);
                        codificarTexto(pilotos.getCode(), codificacoes);

                    }
                    else{
                        arquivo.skipBytes(arquivo.readInt()); //Pulando registro morto
                    }
                    IDloop++;
                }
                catch (EOFException e) {
                    break;
                }
            }

            String binaryPath = "src/data/driversDBCompressao.bin"; //Path do novo arquivo comprimido
            try(BufferedWriter writer = new BufferedWriter(new FileWriter(binaryPath))){
                writer.write(codificado.toString()); //Escrevendo resultado da codificação
                System.out.println("Arquivo comprimido criado!");
                long tamOg = arquivo.length();
                long newSz = binaryPath.length();
                if(tamOg > newSz){
                    System.out.println("Novo arquivo é " + (newSz/tamOg)*100 +"% menor.");
                }
                else{
                    System.out.println("Novo arquivo é " + (tamOg/newSz)*100 +"% maior.");
                }
                System.out.println("Salvando Arvore de Huffman...");
                saveHash(frequencia);
            }

        }
        catch (IOException e){
            System.out.println("Houve um erro ao tentar codificar o arquivo:");
            e.printStackTrace();
        }
    }

    /**
     * Método que fará a leitura do arquivo comprimido
     */
    void descomprimir(){
        if(raiz == null){ //Checando se contem os dados para descompressão
            if(frequencia.isEmpty()){ //Verificando se o Hash está vazio
                frequencia = readHash();
            }
            raiz = construirArvoreHuffman(frequencia); //Carregando caso vazio
        }
        iterar(raiz);
    }

    void iterar(NoDeHuffman raiz){
        StringBuilder str = new StringBuilder();
        String read;
        String binaryPath = "src/data/driversDBCompressao.bin"; //Path do novo arquivo comprimido
        try{
            BufferedReader bf = new BufferedReader(new FileReader(binaryPath));
            read = bf.readLine(); //Lendo todo o arquivo

            for(char c : read.toCharArray()){
                if(c == '0'){
                    raiz = raiz.esquerda;
                } else if (c == '1') {
                    raiz = raiz.direita;
                }
                else{
                    //TODO: TERMINAR ITERAÇÃO E LER TUDO
                }
            }

        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    void contagemDeSimbolos() {
        try{
            arquivo = new RandomAccessFile(dbPath, "rw"); //Abrindo arquivo para realizar contabilização
            arquivo.seek(0); //Apontado para o inicio do arquivo
            Integer metadados = arquivo.readInt(); //Lendo todo o metadado
            Integer IDloop = 1;

            intToHash(metadados); //Inserindo metadados 

            //Leitura do arquivo pós metadados
            while (IDloop <= metadados){
                try{
                    if(arquivo.readChar() != '*'){ //Verificando se será um registro morto. Caso seja, não iremos comprimir pois será espaço inutilizado
                        int tamanhoRegistro = arquivo.readInt(); //Guardando tamanho do registro para enviar ao HashMap
                        byte[] ba = new byte[tamanhoRegistro]; //Criando byte array do tamanho do registro
                        arquivo.readFully(ba); //Lendo todo o registro de acordo com a quantidade de bytes
                        pilotos.fromByteArray(ba); //Extrai o objeto do vetor de btyes

                        pilotosToHash(pilotos); //Inserindo todos os dados dentro do Hash
                    }
                    else{
                        arquivo.skipBytes(arquivo.readInt()); //Pulando registro morto
                    }
                    IDloop++;
                }
                catch (EOFException e) {
                    break;
                }
            }

            //Após o fim do carregamento de todos os pilotos, eles serão inseridos na Árvore de Huffman
            raiz = construirArvoreHuffman(frequencia);
        }
        catch (IOException e){
            System.out.println("Houve um erro ao tentar criar HashMap de incidencia:");
            e.printStackTrace();
        }
    }

    /**
     * Método para construir a árvore binária com base na frequencia
     * 
     * <p>Método utiliza de um PriorityQueue para ordenar todas as entradas e em seguida cria NósDeHuffman
     * para combina-los e montar uma só árvore
     * 
     * @param frequencia - HashMap com todos os simbolos do arquivo contabilizados
     * @return <b>filaPrioritaria.poll()</b> - Árvore binária já balanceada
     */
    NoDeHuffman construirArvoreHuffman(HashMap<Character, Integer> frequencia){
        PriorityQueue<NoDeHuffman> filaPrioritaria = new PriorityQueue<>();

        //Iterando sobre todas entradas do HashMap
        for (Map.Entry<Character, Integer> entry : frequencia.entrySet()){  
            //Recuperando informações
            char simbolo = entry.getKey();
            int freq = entry.getValue();

            //Inserindo na filaPrioritaria
            filaPrioritaria.add(new NoDeHuffman(simbolo, freq));
        }

        //Montando a arvoré binária de Huffman com base na ordem da fila prioritaria
        while (filaPrioritaria.size() > 1) {
            NoDeHuffman esq = filaPrioritaria.poll();
            NoDeHuffman dir = filaPrioritaria.poll();
            NoDeHuffman combinado = new NoDeHuffman(esq.frequencia + dir.frequencia, esq, dir); //Unindo valores de frequencia e designando cada folha
            filaPrioritaria.add(combinado);
        }

        return filaPrioritaria.poll();
    }
    
    /**
     * Algoritmo para converter um Int para uma sequência de char
     * 
     * <p>A conversão de Int para char permite que o número seja inserido no HashMap para a contabilização de sua frequencia
     * @param n com o numero a ser convertido
     */
    void intToHash(Integer n) {
        String simbolo = n.toString();
        char[] c = new char[simbolo.length()];
        for(int i = 0; i < simbolo.length(); i++){
            c[i] = simbolo.charAt(i); //Pegando o símbolo da String e jogando para um char
            inserirNoHash(c[i]); //Inserindo simbolo no Hash
        }
    }

    /**
     * Algoritmo par converter uma String em uma sequencia de Char
     * 
     * <p>A conversão da String para char permite com que seja contabilizado todas as letras da String de maneira
     * que seja possível serem inseridas no HashMap
     * @param simbolo
     */
    void stringToHash(String simbolo){
        char []c = new char[simbolo.length()];
        for(int i = 0; i < simbolo.length(); i++){
            c[i] = simbolo.charAt(i); //Pegando o símbolo da String e jogando para um char
            inserirNoHash(c[i]); //Inserindo simbolo no Hash
        }
    }

    /**
     * Algoritmo para inserir char no Hash
     * 
     * <p>Será conferido se o simbolo já existe dentro  do HashMap, caso exista ele será incrementado,
     * caso não existe, será inserido com a frequencia base de 1.
     * @param c
     */
    void inserirNoHash(Character c) {
        if (!frequencia.containsKey(c)) { //Checando se o HashMap já contem o símbolo
            frequencia.put(c, 1); //Como o Hash não contem o símbolo, ele será adicionado com a frequencia de 1
        }
        else{
            Integer valorInt = frequencia.get(c); //Caso o simbolo existe, ele será pego do Hash
            valorInt++; //Aumentando sua frenquencia
            frequencia.put(c, valorInt); //E devolvido com o Int incrementado
        }
    }


    /**
     * Método para converter todos os dados de um piloto para Simbolos dentro do hash
     * @param pilotos
     */
    void pilotosToHash(driverNode pilotos){

        /*
         * Todas as classes de driverNode:
         *  public int ID;
         *  private String reference;
         *  private String name;
         *  private String surname; 
         *  private String nationality;
         *  private String driverNum;
         *  private LocalDate date;
         *  private String code;
         */

        intToHash(pilotos.getID());
        stringToHash(pilotos.getReference());
        stringToHash(pilotos.getName());         
        stringToHash(pilotos.getSurname());
        stringToHash(pilotos.getNatiotanlity());
        stringToHash(pilotos.getDriverNumber());
        stringToHash(pilotos.getCode());
        stringToHash(pilotos.getDate().toString());
         
    }

    /**
     * Método que gerará todos os códigos de Huffman para os símbolos
     *
     * @param no - para construir a árvore
     * @param codigo - geração do código do simbolo
     * @param codigoHuffman - Para guardar suas entradas
     */
    void codificacaoDeHuffman(NoDeHuffman no, String codigo, HashMap<Character, String> codigoHuffman){
        if(no != null){
            if(no.esquerda == null && no.direita == null){
                codigoHuffman.put(no.simbolo, codigo);
            }

            codificacaoDeHuffman(no.esquerda, codigo + "0", codigoHuffman);
            
            
            codificacaoDeHuffman(no.direita, codigo + "1", codigoHuffman);
        }
    }

    /**
     * Método para realizar a leitura de uma palavra e a codificar
     *
     * <p>Método vai comparar todas as letras de uma palavra e receber seu código de Huffman, código este
     * que serpa inserido em uma String Builder que psoteriormente será escrita
     *
     * @param entrada - palavra a ser codificada
     * @param codigoHuffman - Hash com todos os códigos de Huffman
     */
    void codificarTexto(String entrada, HashMap<Character, String> codigoHuffman ){

        for (char simbolo : entrada.toCharArray()){ //Iterando sobre todas as letras do texto lido
            if(codigoHuffman.containsKey(simbolo)){
                codificado.append(codigoHuffman.get(simbolo)); //Recebendo valor binário da chave
            }
            else{
                System.out.println("ERRO AO CODIFICAR " + simbolo);
            }
        }
    }

    /**
     * Método para salvar o HashMap utilizado para criar a árvore de Huffman
     * @param mapaPrograma contendo o Hash utilizado para criar a arvore
     */
    void saveHash(HashMap<Character, Integer> mapaPrograma){
        if(!mapaPrograma.isEmpty()){
            String path = "src/data/hashmap.dat";
            try{
                ObjectOutputStream objWriter = new ObjectOutputStream(new FileOutputStream(path));
                objWriter.writeObject(mapaPrograma);
                System.out.println("HashMap de codificação salvo!");
            }
            catch (IOException e){
                System.out.println("Houve um erro ao tentar salvar o hash!");
                e.printStackTrace();
            }
        }
        else{
            System.out.println("Não existe nenhum simbolo codificado! Codifique novos simbolos ou carregue um Hash prévio");
        }
    }

    /**
     * Método para realizar leitura de um arquivo de objeto
     *
     * <p>Método faz a leitura de um arquivom que contem um HashMap<Character, Integer> dentro possuindo
     * as informações da árvore binária de Huffman
     * @return hMap
     */
    HashMap<Character, Integer> readHash(){
        HashMap<Character, Integer> hMap = new HashMap<>();
        String path = "src/data/hashmap.dat";
        try{
            ObjectInputStream objReader = new ObjectInputStream(new FileInputStream(path));
            Object objLido = objReader.readObject();
            if (objLido instanceof HashMap){
                hMap = (HashMap<Character, Integer>) objLido;
                System.out.println("HashMap lido com sucesso!");
            }
        }
        catch (IOException | ClassNotFoundException e){
            System.out.println("Houve um erro ao tentar ler o hash! Certifique-se que o arquivo 'hashmap.dat' existe!");
            e.printStackTrace();
        }
        return hMap;
    }
}
