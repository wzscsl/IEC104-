package com.openmuc.j60870.wrapper;

import com.openmuc.j60870.ASduType;
import com.openmuc.j60870.ie.IeQuality;
import com.openmuc.j60870.ie.IeShortFloat;
import com.openmuc.j60870.ie.InformationElement;
import com.openmuc.j60870.ie.InformationObject;

import java.util.Map;

/**
 * @ Author     ：dengzhihai
 * @ Date       ：Created in 14:15 2023/5/6
 * @ Description：
 * @ Modified By：
 * @Version:
 */
//短浮点-13-M_ME_NC_1
public class ShortFloatingPointWrapper extends PointWrapper{
    private IeShortFloat ieValue;
    private IeQuality ieQuality;
    private float srcValue;

    public ShortFloatingPointWrapper(Map<String,String> pointMap, int address, String name, ASduType aSduType,float deadLine){
        super(pointMap,address,name,aSduType,deadLine);
        ieValue = new IeShortFloat(0);
        ieQuality = new IeQuality(false, false, false, false, true);
        informationObject = new InformationObject(address, new InformationElement[][]{{ieValue, ieQuality}});
    }

    public void set(Object data,boolean invalid){
        float newValue = Float.parseFloat(data.toString());
        float diff = srcValue - newValue;
        if(Math.abs(diff)>deadLine){
            setChangeFlag();
        }
        ieValue.setValue(newValue);
        srcValue = newValue;
        ieQuality.setInvalid(invalid);
    }

    public void setInvalid(boolean invalid) {
        boolean oldInvalid =ieQuality.isInvalid();
        ieQuality.setInvalid(invalid);
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
        return srcValue;
    }

}
