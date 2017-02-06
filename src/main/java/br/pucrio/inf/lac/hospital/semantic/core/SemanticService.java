package br.pucrio.inf.lac.hospital.semantic.core;

import br.pucrio.inf.lac.hospital.semantic.data.Visit;
import br.pucrio.inf.lac.hospital.semantic.database.SemanticDao;
import br.pucrio.inf.lac.hospital.semantic.database.SemanticDaoImpMariaDB;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

import java.util.logging.Level;

import lac.cnet.sddl.objects.ApplicationObject;
import lac.cnet.sddl.objects.Message;
import lac.cnet.sddl.objects.PrivateMessage;
import lac.cnet.sddl.udi.core.SddlLayer;
import lac.cnet.sddl.udi.core.UniversalDDSLayerFactory;
import lac.cnet.sddl.udi.core.listener.UDIDataReaderListener;

public class SemanticService implements UDIDataReaderListener<ApplicationObject> {
    
    /** The SDDL core */
    private SddlLayer  core;
    
    /** JSON Mapper. */
    private static ObjectMapper jsonMapper;
    
    /** Data Access */
    private SemanticDao dao;
    
    public static void main(String[] args) {
        java.util.logging.Logger.getLogger("").setLevel(Level.OFF);
 
        new SemanticService();
    }
    
    public SemanticService() {
        
        core = UniversalDDSLayerFactory.getInstance();
        core.createParticipant(UniversalDDSLayerFactory.CNET_DOMAIN);
 
        core.createPublisher();
        core.createSubscriber();
        
        SemanticDao dao = new SemanticDaoImpMariaDB();
 
        Object receiveMessageTopic = core.createTopic(Message.class, Message.class.getSimpleName());
        core.createDataReader(this, receiveMessageTopic);
 
        Object toMobileNodeTopic = core.createTopic(PrivateMessage.class, PrivateMessage.class.getSimpleName());
        core.createDataWriter(toMobileNodeTopic);
 
        System.out.println("=== Server Started (Listening) ===");
    
        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
  
    public void onNewData(ApplicationObject topic){
        Message msg = (Message) topic;
        String rawJSON = new String (msg.getContent());
        
        try {
            JsonNode json = jsonMapper.readValue(rawJSON, JsonNode.class);
            
            Visit v = new Visit();
            v.setHospitalID(json.get("unidade").asLong());
            v.setSpecialtyID(json.get("setor_atendimento").asLong());
            v.setSpecialtyScore((byte)json.get("avaliacao_atendimento").asInt());
            v.setHospitalScore((byte)json.get("avaliacao_hospital").asInt());
            
            if(json.has("motivo_ida_hospital"))
                v.setReportedSymptoms(json.get("motivo_ida_hospital").asText());
            
            if(json.has("problema_diagnosticado"))
                v.setDiagnostic(json.get("problema_diagnosticado").asText());
            
            System.out.println(v);
            
            dao.insertVisit(v);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}