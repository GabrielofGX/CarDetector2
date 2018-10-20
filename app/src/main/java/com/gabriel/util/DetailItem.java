package com.gabriel.util;

/**
 * Created by Administrator off 2017/6/6/011.
 */
public class DetailItem {

    private String id;
    private String item;
    private String result = "";

    public DetailItem(){
    }

    public DetailItem(String id, String item, String result){
        this.id = id;
        this.item = item;
        this.result = result;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    @Override
    public String toString(){
        return "id = " + id + " , item = " + item + " , result = " + result;
    }

}
