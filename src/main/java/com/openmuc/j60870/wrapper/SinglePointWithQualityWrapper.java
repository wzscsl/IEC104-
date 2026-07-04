package com.openmuc.j60870.wrapper;

import com.openmuc.j60870.ASduType;
import com.openmuc.j60870.ie.IeSinglePointWithQuality;
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
//单点遥信-1-M_SP_NA_1  1 - Single-point information without time tag
public class SinglePointWithQualityWrapper extends PointWrapper{
    private IeSinglePointWithQuality ieValue;
    private Boolean srcValue;

    public SinglePointWithQualityWrapper(Map<String,String> pointMap, int address, String name, ASduType aSduType,float deadLine){
        super(pointMap,address,name,aSduType,deadLine);
        ieValue = new IeSinglePointWithQuality(false, false, false, false, true);
        srcValue = false;
        informationObject = new InformationObject(address, new InformationElement[][]{{ieValue}});
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
        ieValue.setInvalid(invalid);
    }

    public void setInvalid(boolean invalid) {
        boolean oldInvalid =ieValue.isInvalid();
        ieValue.setInvalid(invalid);
        if(oldInvalid != invalid)
            setChangeFlag();

    }

    public void set(Object data){
       set(data,false);
    }

    public Object get(){
        return srcValue;
    }

    public Number getNumber(){
        return srcValue==true?1:0;
    }

}
