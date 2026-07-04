package com.openmuc.j60870.wrapper;

import com.openmuc.j60870.ASduType;
import com.openmuc.j60870.ie.InformationElement;
import com.openmuc.j60870.ie.InformationObject;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @ Author     ：dengzhihai
 * @ Date       ：Created in 14:14 2023/5/6
 * @ Description：
 * @ Modified By：
 * @Version:
 */
@Data
public class PointWrapper{
    protected int address;
    protected String name;
    protected ASduType aSduType;
    protected double scaleIndex;
    //当前单个点实列化了一个InformationObject存放其关联的数据
    protected InformationObject informationObject;
    //当前单个点所在批量上送的信息结构
    protected InformationObject batchInformationObject;
    protected Map<String,String> pointMap;
    protected float deadLine; //死区

    public PointWrapper(){

    }

    public PointWrapper(Map<String,String> pointMap,int address,String name,ASduType aSduType,float deadLine){
        this.pointMap = pointMap;
        this.address = address;
        this.name = name;
        this.aSduType = aSduType;
        this.deadLine = deadLine;
    }


    //发送数据，将数据写入到当前点内
    public void set(Object data){

    }

    //发送数据，将数据写入到当前点内
    public void set(Object data,boolean invalid){

    }

    public void setInvalid(boolean invalid) {}

    //遥控点的更新是从外部的ie点更新到里面来，所以是接收
    public void write(Object data){

    }

    public Object get(){
        return 0;
    }

    public Number getNumber(){
        return 0;
    }

    //填充数组
    public void fillBuffer(List<InformationElement[]> informationElements){
        informationElements.add(informationObject.getInformationElements()[0]);
    }

    public void setBatchInformationObject(InformationObject batchInformationObject){
        this.batchInformationObject = batchInformationObject;
    }

    public void setChangeFlag(){
        if(batchInformationObject!=null)
            batchInformationObject.setChangeFlag(1);
    }
}
