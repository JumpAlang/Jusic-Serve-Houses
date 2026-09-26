package com.scoder.jusic.model;

import com.scoder.jusic.service.ConfigService;
import lombok.Data;

/** Public room settings; contains no passwords or administrator credentials. */
@Data
public class RoomState {
    private String houseId;
    private Float voteRate;
    private boolean goodModel;
    private boolean randomModel;
    private boolean musicCircleModel;
    private boolean listCircleModel;
    private boolean enableSearch;
    private boolean enableSwitch;

    public static RoomState read(ConfigService config, String houseId) {
        RoomState state = new RoomState();
        state.setHouseId(houseId);
        state.setVoteRate(config.getVoteRate(houseId));
        state.setGoodModel(Boolean.TRUE.equals(config.getGoodModel(houseId)));
        state.setRandomModel(Boolean.TRUE.equals(config.getRandomModel(houseId)));
        state.setMusicCircleModel(Boolean.TRUE.equals(config.getMusicCircleModel(houseId)));
        state.setListCircleModel(Boolean.TRUE.equals(config.getListCircleModel(houseId)));
        // Match command authorization: only an explicit false disables guests.
        state.setEnableSearch(!Boolean.FALSE.equals(config.getEnableSearch(houseId)));
        state.setEnableSwitch(!Boolean.FALSE.equals(config.getEnableSwitch(houseId)));
        return state;
    }
}
