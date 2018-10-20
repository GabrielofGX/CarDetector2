package com.gabriel.util;

/**
 * Created by Administrator off 2017/5/11/011.
 */
public class DetectItem {

    private int id;
    private String item;
    private String result = "";
    private int type; //用于标记是手动检测项，还是自动检测项，type==2表示手动检测项



    public DetectItem(){

    }

    public DetectItem(int id, String item, String result){
        this.id = id;
        this.item = item;
        this.result = result;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public String toString(){
        return "id = " + id + " , item = " + item + " , result = " + result;
    }

}
