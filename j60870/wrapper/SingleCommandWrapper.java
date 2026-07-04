package com.openmuc.j60870.wrapper;

import com.openmuc.j60870.ASduType;
import com.openmuc.j60870.ie.IeSingleCommand;
import com.openmuc.j60870.ie.InformationElement;
import com.openmuc.j60870.ie.InformationObject;

import java.util.Map;

/**
 * @ Author     ：dengzhihai
 * @ Date       ：Created in 14:16 2023/5/6
 * @ Description：
 * @ Modified By：
 * @Version:
 */
//单点遥控-45-C_SC_NA_1
public class SingleCommandWrapper extends PointWrapper{
    private IeSingleCommand ieValue;
    private boolean srcValue;

    public SingleCommandWrapper(Map<String,String> pointMap, int address, String name, ASduType aSduType,float deadLine){
        super(pointMap,address,name,aSduType,deadLine);
        ieValue = new IeSingleCommand(false, 1, false);
        srcValue = false;
        informationObject = new InformationObject(address, new InformationElement[][]{{ieValue}});
    }

    //遥控点的更新是从ie点到外
    public void write(Object data){
        boolean newValue = Float.parseFloat(data.toString())>0?true:false;
        if( newValue != srcValue){
            ieValue.setValue(newValue);
            setChangeFlag();
        }
        srcValue = newValue;
    }

    public void set(Object data,boolean invalid){
        //srcValue = (Boolean)data;
        //ieValue.setValue(srcValue);
        boolean newValue = Float.parseFloat(data.toString())>0?true:false;
        //boolean newValue = (boolean)data;
        if( newValue != srcValue) {
            ieValue.setValue(newValue);
            setChangeFlag();
        }
        srcValue = newValue;
    }

    public Object get(){
        return srcValue;
    }

    public Number getNumber(){
        return srcValue==true?1:0;
    }


}
