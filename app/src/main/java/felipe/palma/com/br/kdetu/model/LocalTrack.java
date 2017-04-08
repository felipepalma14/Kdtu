package felipe.palma.com.br.kdetu.model;

import com.google.firebase.database.Exclude;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Roberlandio on 06/02/2017.
 */

public class LocalTrack implements Serializable{
    private String id;
    private String descricao;
    private double latitude;
    private double longitude;
    private String atualizacao;

    public LocalTrack() {
    }

    public LocalTrack(String id, String descricao, double latitude, double longitude, String atualizacao) {
        this.id = id;
        this.descricao = descricao;
        this.latitude = latitude;
        this.longitude = longitude;
        this.atualizacao = atualizacao;
    }

    public LocalTrack(String id, String descricao, double latitude, double longitude) {
        this.id = id;
        this.descricao = descricao;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public LocalTrack(String descricao, double latitude, double longitude) {
        this.descricao = descricao;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAtualizacao() {
        return atualizacao;
    }

    public void setAtualizacao(String atualizacao) {
        this.atualizacao = atualizacao;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }



    @Exclude
    public Map<String,Object> toMap(){

        HashMap<String,Object>result = new HashMap<>();
        result.put("id",id);
        result.put("descricao",descricao);
        result.put("latitude",latitude);
        result.put("longitude",longitude);

        return result;
    }
}
