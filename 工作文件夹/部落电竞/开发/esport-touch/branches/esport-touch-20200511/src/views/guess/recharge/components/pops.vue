<template>
  <div class="ui_pop" v-if="showFlag">
    <!-- 确认支付弹窗 -->
    <div class="confirm_pay" v-if="showType == 1">
      <a class='close' @click="closePop"></a>
      <h3>确认支付</h3>
      <p>您将支付{{currChargeItem.money}}元购买获得{{currChargeItem.starNum}}预测星星</p>
      <div class="pay_btn">
        <!-- 加disabled置为不可点击 -->
        <a class="disabled" @click="toQrcodePay(currChargeItem.money)">立即支付{{currChargeItem.money}}元</a>
      </div>
    </div>

    <!-- 支付状态弹窗 -->
    <div class="payStatus" v-if="showType == 2">
      <a class="close" @click="closePop"></a>
      <div v-if="payType == 1">
        <div class="title"><span class="failed_icon"></span>支付中，请稍后查看余额</div>
        <!-- <p>您已成功购买{{currChargeItem.starNum}}预测星星</p> -->
      </div>
      <div v-if="payType == 2">
        <div class="title"><span class="success_icon"></span>支付成功</div>
        <p>您已成功购买{{currChargeItem.starNum}}预测星星</p>
      </div>
      <div v-if="payType == 3">
        <div class="title"><span class="failed_icon"></span>支付失败</div>
        <!-- <p>您已成功购买{{currChargeItem.starNum}}预测星星</p> -->
      </div>

      <div v-if="payType == 4">
        <div class="title"><span class="sad_icon"></span>余额不足</div>
        <a class="recharge_btn">星星充值</a>
      </div>
      <div v-if="payType == 5">
        <div class="title"><span class="happy_icon"></span>下单成功</div>
        <p>下单结果详见预测记录</p>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  components: {},
  props: ["showType", "showFlag", "currChargeItem"],
  data() {
    return {
      prizeDay: [],
      payType: Number,
      chargeMoneyList: [], //充值金额列表
      // currChargeItem: null, //当前选择的充值选项
      chargeResParam: null,
      walletRec: null //用户星星数量
    };
  },
  methods: {
    closePop() {
      this.showType = 0;
      this.$emit("closePop");
    },
    //支付完成检查支付状态
    checkChargeOrder(payNo) {
      let param = {};
      param.payNo = payNo;
      return this.$post("/api/payment/getThirdPayStatus", param)
        .then(rsp => {
          const dataResponse = rsp;
          if (dataResponse.code == 200) {
            this.showType = 2;
            if (dataResponse.data.status == 1) {
              this.payType = 1;
            } else if (dataResponse.data.status == 2) {
              //支付成功
              this.payType = 2;
              this.walletRec = dataResponse.data.giftRecScore;
              this.$emit("updateUserScore", dataResponse.data.giftRecScore);
            } else {
              this.payType = 3;
            }
          } else {
            this.$toast(dataResponse.message);
          }
        })
        .catch(error => {
          console.log(error);
        });
    },
    toQrcodePay(money) {
      this.payType = 2;
      let param = {};
      param.chargeAmount = money;
      param.chargeWay = 5;
      param.chargeType = 1;
      return this.$post("/api/h5charge/tocharge", param)
        .then(rsp => {
          const dataResponse = rsp;
          if (dataResponse.code == 200) {
            console.log(dataResponse);
            let link = dataResponse.data.requestUrl;
            this.chargeResParam = dataResponse.data;
            this.showType = 2;
          } else if (dataResponse.code == 9999) {
            this.$toast(dataResponse.message);
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
@import "../../../../assets/common/_base";
@import "../../../../assets/common/_mixin";

.close {
  position: absolute;
  top: 1.6vw;
  right: 2.67vw;
  width: 5.33vw;
  height: 5.33vw;
  @include getBgImg("../../../../assets/images/guess/close.png");
}

.success_icon,
.failed_icon {
  width: 9.07vw;
  height: 9.07vw;
  margin-right: 2.66vw;
}

.failed_icon {
  @include getBgImg("../../../../assets/images/guess/failed_icon.png");
}

.success_icon {
  @include getBgImg("../../../../assets/images/guess/success_icon.png");
}

.confirm_pay,
.qrCodePay,
.payStatus {
  position: relative;
  width: 100%;
  margin: 0 2.66vw;
  text-align: center;
  background-color: #261314;
}

.confirm_pay {
  h3 {
    font-size: 4.8vw;
    line-height: 8.8vw;
    color: #fedcd7;
    font-weight: bold;
    background-color: #2e1a1b;
  }
  p {
    padding-top: 17.33vw;
    color: #f58079;
    font-size: 4.8vw;
  }
  .pay_btn {
    margin-top: 21.33vw;
    padding: 2.66vw 0 4vw;
    @include getBgImg("../../../../assets/images/guess/shadow.png");
    background-size: 100% auto;
    background-position: top center;
    a {
      display: block;
      width: 14.8vw;
      margin: 0 auto;
      border-radius: 3px;
      line-height: 8.8vw;
      font-size: 3.73vw;
      color: #fff;
      background: linear-gradient(to bottom, #df2f26, #86171a);
      background: -webkit-linear-gradient(top, #df2f26, #86171a);
    }
    .disabled {
      opacity: 0.2;
    }
  }
}

.payStatus {
  @extend .flex_v_h;
  height: 46.67vw;
  color: #fff;

  .title {
    @extend .flex_v_h;
    font-size: 5.33vw;
  }
  p {
    padding-top: 6.13vw;
    font-size: 3.73vw;
  }
  .recharge_btn {
    @extend .flex_v_h;
    margin: 5.33vw auto 0;
    width: 42.13vw;
    height: 10.13vw;
    color: #f5b457;
    border: 3px solid #f5b457;
  }
}
</style>
