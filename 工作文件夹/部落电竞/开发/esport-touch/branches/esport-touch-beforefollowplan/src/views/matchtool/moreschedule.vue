<template>
  <div class="Page" :style="'background-image: url('+teamRoomInfo.bgUrl+')'">
    <header class="mod_header">
      <nav-bar :pageTitle="'更多赛程'"></nav-bar>
      <div class="room_title">
        <title>{{teamRoomInfo.name}}</title>
      </div>
    </header>
    <div class="main">
      <h3>
        <div class="flex_hc">
          <span>赛程</span>
          <span class="format">[ {{teamRoomInfo.matchTeamNum}}支队伍
            ，{{matchTypeName[teamRoomInfo.matchType]}}]</span>
        </div>
      </h3>
      <ul>
        <schedule-list ref="scheduleList" :matchId="matchId" :scheduleList="teamRoomInfo.scheduleInfo"
          :creatorFlag="teamRoomInfo.creator">
        </schedule-list>

      </ul>
    </div>
  </div>
</template>

<script>
import navBar from "../../components/header/nav_bar/index.vue";
import scheduleList from "./components/schedulelist.vue";

export default {
  components: { scheduleList, navBar },
  props: [],

  data() {
    return {
      isIosApp: false,
      matchId: Number,
      userTeamId: Number,
      teamRoomInfo: {},
      matchTypeName: { 1: "淘汰制", 2: "循环制" }
    };
  },
  mounted() {
    this.matchId = this.$route.query.matchId;
    this.getMatchRoomTeamInfo();
    if (this.$route.query.clientType == 4) {
      this.isIosApp = true;
    }
  },
  methods: {
    getMatchRoomTeamInfo() {
      let param = {};
      param.pageType = 2;
      return this.$post("/api/matchtool/matchRoom/" + this.matchId, param)
        .then(rsp => {
          const dataResponse = rsp;
          if (dataResponse.code == "200") {
            this.teamRoomInfo = dataResponse.data.teamRoomInfo;
            if (this.teamRoomInfo.scheduleList.length > 0) {
              this.teamRoomInfo.scheduleInfo = this.teamRoomInfo.scheduleList;
            }
            this.userTeamId = this.teamRoomInfo.userTeamId;
            this.$refs.scheduleList.matchStatus = this.teamRoomInfo.matchStatus;
          } else {
            this.$toast("系统异常");
          }
        })
        .catch(error => {
          console.log(error);
        });
    }
  }
};
</script>

<style lang='scss' scoped>
@import "../../assets/common/_mixin";
@import "../../assets/common/_base";

.Page {
  background-position: center 44px;
  background-repeat: no-repeat;
  background-size: 100% auto;
  &.iosApp {
    @media only screen and (min-device-width: 375px) and (min-device-height: 812px) and (-webkit-device-pixel-ratio: 3),
      only screen and (device-width: 414px) and (device-height: 896px) and (-webkit-device-pixel-ratio: 2) {
      background-position: center 88px;
    }
  }
}

.mod_header {
  background: transparent;
  .nav_bar {
    background-color: #fff;
  }
}

.room_title {
  padding: 22.1333vw 13.4667vw 1.3333vw;
  text-align: center;
  background-size: 100% auto;
  background-repeat: no-repeat;
  background-position: top center;
  title {
    display: inline-block;
    padding: 0 5.3333vw;
    line-height: 8vw;
    font-size: 4vw;
    font-weight: 600;
    color: #fff;
    background-color: rgba(0, 0, 0, 0.6);
    border-radius: 4vw;
  }
}

h3 {
  @extend .flex_v_justify;
  padding: 2.9333vw;
  font-size: 5.3333vw;
  line-height: 5.8667vw;
  color: #fff;
  font-weight: normal;
  .format {
    padding-left: 2.4vw;
    font-size: 3.4667vw;
    color: #ff7e00;
  }
}
</style>
