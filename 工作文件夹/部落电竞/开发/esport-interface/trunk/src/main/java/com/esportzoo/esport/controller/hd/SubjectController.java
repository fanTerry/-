package com.esportzoo.esport.controller.hd;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.esportzoo.common.appmodel.domain.result.ModelResult;
import com.esportzoo.common.redisclient.RedisClient;
import com.esportzoo.esport.connect.request.BaseRequest;
import com.esportzoo.esport.connect.request.hd.SubjectRequest;
import com.esportzoo.esport.connect.response.CommonResponse;
import com.esportzoo.esport.connect.response.hd.Hd101GiftResponse;
import com.esportzoo.esport.connect.response.hd.SubjectResponse;
import com.esportzoo.esport.constant.ResponseConstant;
import com.esportzoo.esport.constants.ClientType;
import com.esportzoo.esport.controller.BaseController;
import com.esportzoo.esport.domain.UserConsumer;
import com.esportzoo.esport.hd.constants.HdCode;
import com.esportzoo.esport.hd.entity.HdSubjectLog;
import com.esportzoo.esport.hd.share.HdUserShareLogServiceClient;
import com.esportzoo.esport.hd.vo.HdUserShareLogVo;
import com.esportzoo.esport.manager.hd.SubjectManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @description: 用户答题相关接口
 *
 * @author: Haitao.Li
 *
 * @create: 2019-09-16 14:40
 **/
@Controller
@RequestMapping("subject")
@Api(value = "题目相关数据接口", tags = {"题目相关数据接口"})
public class SubjectController extends BaseController {


	public static final String logPrefix = "题目相关数据接口";

	@Autowired
	SubjectManager subjectManager;
	@Autowired
	private HdUserShareLogServiceClient hdUserShareLogClient;

	@Autowired
	RedisClient redisClient;


	/**
	 * 进入答题首页接口数据
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/home", produces = "application/json; charset=utf-8", method = RequestMethod.POST)
	@ApiOperation(value = "答题首页", httpMethod = "POST", consumes = "application/x-www-form-urlencoded", produces = "application/json")
	@ApiResponse(code = 200, message = "答题首页", response = CommonResponse.class)
	public @ResponseBody
	CommonResponse<Hd101GiftResponse> homePage(HttpServletRequest request, String shareCode, BaseRequest baseRequest) {
		UserConsumer userConsumer = getLoginUsr(request);
		Hd101GiftResponse homeGift=null;
		if (userConsumer == null) {
			return CommonResponse.withErrorResp("用户未登录");
		}
		logger.info(logPrefix + "进入活动首页,用户id={},shareCode={}", userConsumer.getId(), shareCode);
		try {
			if (StringUtils.isNotBlank(shareCode)) {// 带了邀请码,需要绑定邀请关系,是是否邀请成功的依据
				HdUserShareLogVo vo = new HdUserShareLogVo();
				vo.setRespondUserId(userConsumer.getId().intValue());
				vo.setShareCode(shareCode);
				ModelResult<Integer> modelResult = hdUserShareLogClient.addUserClickShareUrlLog(vo);
				logger.info(logPrefix + "进入活动首页,生成绑定关系,用户id={},接口返回={}", userConsumer.getId(), JSONObject.toJSONString(modelResult));
			}
			if (null!=baseRequest.getClientType()&& ClientType.WXXCY.getIndex()==baseRequest.getClientType().intValue()){
				homeGift=subjectManager.getWxxcxGomeGift(baseRequest);
			}else {
				homeGift= subjectManager.getHomeGift();
			}
			return CommonResponse.withSuccessResp(homeGift);
		} catch (Exception e) {
			logger.info(logPrefix + "进入活动首页出现异常，用户id：{},异常信息:{}", userConsumer.getId(), e.getMessage(),e);
			return CommonResponse.withResp(ResponseConstant.SYSTEM_ERROR_CODE, ResponseConstant.SYSTEM_ERROR_MESG);

		}

	}


	/**
	 * 点击参加答题活动
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/joinSubject", produces = "application/json; charset=utf-8", method = RequestMethod.POST)
	@ApiOperation(value = "参与答题", httpMethod = "POST", consumes = "application/x-www-form-urlencoded", produces = "application/json")
	@ApiResponse(code = 200, message = "参与答题", response = CommonResponse.class)
	public @ResponseBody CommonResponse<SubjectResponse> joinSubject(HttpServletRequest request, SubjectRequest subjectRequest) {
		UserConsumer userConsumer = getLoginUsr(request);
		if (userConsumer == null) {
			return CommonResponse.withErrorResp("用户未登录");
		}
		// 点击参加按钮防从
		String repeatClickKey = "subject_hd." + userConsumer.getId() + "." + HdCode.HD_101.getIndex();
		if (!redisClient.setNX(repeatClickKey, "1", 2)) {
			logger.error("用户id[{}],nickName=[{}],参与活动code[{}],请求频繁", userConsumer.getId(), userConsumer.getNickName(),
					HdCode.HD_101.getDescription());
			return CommonResponse.withResp("4444", "请勿重复点击");
		}
		try {
			subjectRequest.setConsumerId(userConsumer.getId());
			logger.info("用户：【{}】，参与答题joinSubject,传入参数{}", subjectRequest.getConsumerId(), JSONObject.toJSONString(subjectRequest));
			CommonResponse<SubjectResponse> commonResponse = subjectManager.joinSubject(subjectRequest);
			return commonResponse;
		} catch (Exception e) {
			logger.info(logPrefix + "joinSubject出现异常，用户id：{},异常信息:{}", userConsumer.getId(), e);
			return CommonResponse.withResp(ResponseConstant.SYSTEM_ERROR_CODE, ResponseConstant.SYSTEM_ERROR_MESG);

		}
	}

	/**
	 * 获取答题页开始题目
	 * 必传参数 hdUserLogId ，subjectId
	 * @param request
	 * @param subjectRequest
	 * @return
	 */
	@RequestMapping(value = "/startGame", produces = "application/json; charset=utf-8", method = RequestMethod.POST)
	@ApiOperation(value = "开始答题", httpMethod = "POST", consumes = "application/x-www-form-urlencoded", produces = "application/json")
	@ApiResponse(code = 200, message = "开始答题", response = CommonResponse.class)
	public @ResponseBody
	CommonResponse<SubjectResponse> startGame(HttpServletRequest request, SubjectRequest subjectRequest) {
		UserConsumer userConsumer = getLoginUsr(request);
		if (userConsumer == null) {
			return CommonResponse.withErrorResp("用户未登录");
		}
		logger.info(logPrefix + "用户【{}】开始答题，获取第一道题目，参数param 【{}】", userConsumer.getId(), JSON.toJSONString(subjectRequest));
		try {
			subjectRequest.setConsumerId(userConsumer.getId());
			return subjectManager.getFirstSubject(subjectRequest);
		} catch (Exception e) {
			logger.info(logPrefix + "startGame出现异常，用户id：{},异常信息:{}", userConsumer.getId(), e);
			e.printStackTrace();
			return CommonResponse.withResp(ResponseConstant.SYSTEM_ERROR_CODE, ResponseConstant.SYSTEM_ERROR_MESG);
		}


	}

	/**
	 * 结束答题
	 * @param request
	 * @param subjectRequest
	 * @return
	 */
	@RequestMapping(value = "/endGame", produces = "application/json; charset=utf-8", method = RequestMethod.POST)
	@ApiOperation(value = "结束答题", httpMethod = "POST", consumes = "application/x-www-form-urlencoded", produces = "application/json")
	@ApiResponse(code = 200, message = "结束答题", response = CommonResponse.class)
	public @ResponseBody
	CommonResponse<SubjectResponse> endGame(HttpServletRequest request, SubjectRequest subjectRequest) {
		UserConsumer userConsumer = getLoginUsr(request);
		if (userConsumer == null) {
			return CommonResponse.withErrorResp("用户未登录");
		}
		// 点击参加按钮防从
		String repeatClickKey = "endGame_subject_hd." + userConsumer.getId() + "." + HdCode.HD_101.getIndex();
		if (!redisClient.setNX(repeatClickKey, "1", 2)) {
			logger.error("用户id[{}],nickName=[{}],参与活动code[{}],请求频繁", userConsumer.getId(), userConsumer.getNickName(),
					HdCode.HD_101.getDescription());
			return CommonResponse.withResp("4444", "请勿重复点击");
		}
		if (subjectRequest==null){
			logger.info(logPrefix + "用户【{}】结束答题，subjectRequest.is.null", userConsumer.getId());
			return CommonResponse.withErrorResp("结束答题参数异常");
		}
		subjectRequest.setConsumerId(userConsumer.getId());
		logger.info(logPrefix + "用户【{}】结束答题，获取第一道题目，参数param 【{}】", userConsumer.getId(), JSON.toJSONString(subjectRequest));
		try {

			CommonResponse<SubjectResponse> responseCommonResponse = subjectManager.endGame(subjectRequest);
			return responseCommonResponse;

		} catch (Exception e) {
			logger.info(logPrefix + "endGame出现异常，用户id：{},异常信息:{}", userConsumer.getId(), e);
			e.printStackTrace();
			return CommonResponse.withResp(ResponseConstant.SYSTEM_ERROR_CODE, ResponseConstant.SYSTEM_ERROR_MESG);
		}


	}


	/**
	 * 校验答案并返回下一道题目
	 * 必传参数 hdUserLogId  ，userOptionId
	 * @param request
	 * @param subjectRequest
	 * @return 校
	 */
	@RequestMapping(value = "/verifyAnswer", produces = "application/json; charset=utf-8", method = RequestMethod.POST)
	@ApiOperation(value = "校验答案", httpMethod = "POST", consumes = "application/x-www-form-urlencoded", produces = "application/json")
	@ApiResponse(code = 200, message = "校验答案", response = CommonResponse.class)
	public @ResponseBody
	CommonResponse<SubjectResponse> verifyAnswer(HttpServletRequest request, SubjectRequest subjectRequest) {
		UserConsumer userConsumer = getLoginUsr(request);
		if (userConsumer == null) {
			return CommonResponse.withErrorResp("用户未登录");
		}

		String repeatClickKey ="verifyAnswer_subjectid_"+subjectRequest.getSubjectId()+"subjectidLogId_"+subjectRequest.getSubjectLogId();
		if (!redisClient.setNX(repeatClickKey,"1",3)){
			logger.info("点击答案过于频繁：userConsumer【{}】-SubjectId【{}】- subjectidLogId【{}】",userConsumer.getId(),subjectRequest.getSubjectId(),subjectRequest.getSubjectLogId());
			return CommonResponse.withErrorResp("请勿重复点击");
		}

		try {
			subjectRequest.setConsumerId(userConsumer.getId());
			/** 校验答案 */
			boolean isRight = subjectManager.verifyAnswer(subjectRequest);
			logger.info(logPrefix + "校验答案{},参数【{}】", isRight,JSONObject.toJSONString(subjectRequest));
			/** 修改题目状态 */
			HdSubjectLog hdSubjectLog = subjectManager.updateSubjectStatusByAnswer(subjectRequest, isRight);

			if (hdSubjectLog == null) {
				return CommonResponse.withErrorResp("答案校验出现异常");
			}
			/** 处理题目的返回 */
			CommonResponse<SubjectResponse> commonResponse = subjectManager.nextOrOverSubject(subjectRequest, isRight);
			return commonResponse;


		} catch (Exception e) {
			logger.error(logPrefix + "verifyAnswer出现异常，用户id：{},异常信息:{}", userConsumer.getId(), e.getMessage());
			return CommonResponse.withResp(ResponseConstant.SYSTEM_ERROR_CODE, ResponseConstant.SYSTEM_ERROR_MESG);

		}

	}

	@RequestMapping(value = "/getGifts", produces = "application/json; charset=utf-8", method = RequestMethod.POST)
	@ApiOperation(value = "领取奖品", httpMethod = "POST", consumes = "application/x-www-form-urlencoded", produces = "application/json")
	@ApiResponse(code = 200, message = "领取礼品", response = CommonResponse.class)
	public @ResponseBody
	CommonResponse<SubjectResponse> getGifts(HttpServletRequest request, SubjectRequest subjectRequest) {

		UserConsumer userConsumer = getLoginUsr(request);
		if (userConsumer == null) {
			return CommonResponse.withErrorResp("用户未登录");
		}
		subjectRequest.setConsumerId(userConsumer.getId());

		if (subjectRequest.getUserGiftLogId() == null) {
			return CommonResponse.withErrorResp("礼品领取,UserGiftLogId为空");
		}
		if (subjectRequest.getSubjectLogId() == null) {
			return CommonResponse.withErrorResp("礼品领取,SubjectLogId为空");
		}
		try {
			CommonResponse<SubjectResponse> gifts = subjectManager.getGifts(subjectRequest);
			return gifts;
		} catch (Exception e) {
			logger.error(logPrefix + "用户：【{}】，参数param 【{}】异常参数：{}", userConsumer.getId(), subjectRequest.getUserGiftLogId(), e);
			e.printStackTrace();
			return CommonResponse.withErrorResp("领取礼品出现异常");
		}


	}


	/**
	 * 获取礼品接口数据
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/giftList", produces = "application/json; charset=utf-8", method = RequestMethod.POST)
	@ApiOperation(value = "答题礼物列表", httpMethod = "POST", consumes = "application/x-www-form-urlencoded", produces = "application/json")
	@ApiResponse(code = 200, message = "答题礼物列表", response = CommonResponse.class)
	public @ResponseBody
	CommonResponse <JSONObject> getGift(HttpServletRequest request) {
		JSONObject jsonObject = new JSONObject();
		UserConsumer userConsumer = getLoginUsr(request);
		if (userConsumer == null) {
			return CommonResponse.withErrorResp("用户未登录");
		}
		try {
		List<Hd101GiftResponse> giftList= subjectManager.getGift();
		jsonObject.put("giftList", giftList);
		return CommonResponse.withSuccessResp(jsonObject);
		} catch (Exception e) {
			logger.info(logPrefix + "进入活动答题礼物列表，用户id：{},异常信息:{}", userConsumer.getId(), e.getMessage(),e);
			return CommonResponse.withResp(ResponseConstant.SYSTEM_ERROR_CODE, ResponseConstant.SYSTEM_ERROR_MESG);
		}

	}

}
