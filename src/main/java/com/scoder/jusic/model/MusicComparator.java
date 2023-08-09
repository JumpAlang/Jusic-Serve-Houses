package com.scoder.jusic.model;

import java.util.Comparator;

/**
 * @author alang
 * @create 2020-02-06 1:22
 */
public class MusicComparator implements Comparator<Music> {
    @Override
    public int compare(Music o1, Music o2) {
        if(o1.getTopTime() != null && o2.getTopTime() != null){
            return (int)(o1.getTopTime()-o2.getTopTime());
        }else if(o1.getTopTime() != null && o2.getTopTime() == null){
            return 1;
        }else if(o2.getTopTime() != null && o1.getTopTime() == null){
            return -1;
        }else if(o1.getIps().size() == o2.getIps().size()){
            if(o1.getGoodTime() == null && o2.getGoodTime() == null){
                return (int)(o2.getPickTime() - o1.getPickTime());
            }
            if(o1.getGoodTime() == null){
                return -1;
            }
            if(o2.getGoodTime() == null){
                return 1;
            }
            return (int) (o2.getGoodTime() - o1.getGoodTime());
        }else{
            return o1.getIps().size() - o2.getIps().size();
        }
    }
}
