package com.mmall.service.impl;

import com.mmall.common.Const;
import com.mmall.common.ServiceResponse;
import com.mmall.common.TokenCache;
import com.mmall.dao.UserMapper;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * @program: mmall
 * @description: 接口方法
 * @author: Hailong
 * @create: 2018-07-30 15:59
 **/
@Service("iUserService")
public class UserServiceImpl implements IUserService {
    @Autowired
    private UserMapper userMapper;

    @Override
    public ServiceResponse<User> login(String username, String password) {
        int resultCount = userMapper.checkUsername(username);
        if (resultCount == 0) {
            return ServiceResponse.createByErrorMessage("密码错误");
        }

        //todo密码登陆MD5

        String md5Password = MD5Util.MD5EncodeUtf8(password);
        User user = userMapper.selectLogin(username, md5Password);
        if (user == null) {
            return ServiceResponse.createByErrorMessage("密码错误");
        }

        user.setPassword(StringUtils.EMPTY);
        return ServiceResponse.createBySuccess("登陆成功", user);
    }

    /*
    *  用户注册
    *
     *  */
    public ServiceResponse<String> register(User user) {
        ServiceResponse validResponse = this.checkValid(user.getUsername(), Const.USERNAME);
        if (!validResponse.isSuccess()) {
            return validResponse;
        }
        validResponse = this.checkValid(user.getEmail(), Const.EMAIL);
        if (!validResponse.isSuccess()) {
            return validResponse;
        }


        user.setRole(Const.Role.ROLE_CUSTOMER);
        //进行MD5加密
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));

        int resultCount = userMapper.insert(user);
        if (resultCount == 0) {
            return ServiceResponse.createByErrorMessage("注册失败");
        }
        return ServiceResponse.createBySuccessMessage("注册成功");
    }

    /**
     * 进行用户名和email校验
     */
    public ServiceResponse<String> checkValid(String str, String type) { //str是value值，根据type传的是username还是email进行不同的sql语句
        if (StringUtils.isNotBlank(type)) {
            if (Const.USERNAME.equals(type)) {
                int resultCount = userMapper.checkUsername(str);
                if (resultCount > 0) {
                    return ServiceResponse.createByErrorMessage("用户名已经存在");
                }
            }
            if (Const.EMAIL.equals(type)) {
                int resultCount = userMapper.checkEmail(str);
                if (resultCount > 0) {
                    return ServiceResponse.createByErrorMessage("email已经存在");
                }
            }
        } else {
            return ServiceResponse.createBySuccessMessage("参数错误");
        }
        return ServiceResponse.createBySuccessMessage("校验成功");
    }

    public ServiceResponse selectQuestion(String username) {
        ServiceResponse validResponse = this.checkValid(username, Const.USERNAME);
        if (validResponse.isSuccess()) {
            //用户不存在
            return ServiceResponse.createByErrorMessage("用户不存在");
        }
        String question = userMapper.selectQuestionByUsername(username);
        if (StringUtils.isNotBlank(question)) {
            return ServiceResponse.createBySuccessMessage(question);
        }
        return ServiceResponse.createByErrorMessage("找回密码的问题是空的");
    }

    public  ServiceResponse<String> checkAnswer(String username, String question, String answer){
      int resultCount = userMapper.checkAnswer(username,question,answer);
      if (resultCount>0){
          //说明这个问题答案是这个用户的，并且是正确的
          String forgetToken = UUID.randomUUID().toString();
          TokenCache.setKey("token_"+username,forgetToken);
          return ServiceResponse.createBySuccess(forgetToken);
      }
      return ServiceResponse.createByErrorMessage("问题的答案错误");
    }


    public ServiceResponse<String> forgetRestPassword(String username, String passwordNew, String forgetToken){
        if (StringUtils.isBlank(forgetToken)){
            return  ServiceResponse.createByErrorMessage("参数错误，token需要传递");
        }
        ServiceResponse validResponse = this.checkValid(username, Const.USERNAME);
        if (validResponse.isSuccess()) {
            //用户不存在
            return ServiceResponse.createByErrorMessage("用户不存在");
        }

        String token = TokenCache.getKey(TokenCache.TOKEN_PREFIX+username);
            if (StringUtils.isBlank(token)){
                ServiceResponse.createByErrorMessage("token过期或者无效");
            }

        if (StringUtils.equals(forgetToken,token)){
                String md5Password = MD5Util.MD5EncodeUtf8(passwordNew);
                int rowCount = userMapper.updatePasswordByUsername(username,md5Password);

                if (rowCount>0) {
                    return  ServiceResponse.createBySuccessMessage("修改密码成功");
                }

        }else{
            ServiceResponse.createByErrorMessage("token错误,请重新获取重置密码的token");
        }
        return  ServiceResponse.createByErrorMessage("修改密码失败");

    }

    public ServiceResponse<String> resetPassword( String passwordOld, String passwordNew, User user){
        //防止横向越权，检验的时候一定是指定这个用户，如果不指定id的话，可能有相同的密码，这样的话结果是true或者count>0了就
       int resultCount = userMapper.checkPassword(MD5Util.MD5EncodeUtf8(passwordOld),user.getId() );
       if (resultCount == 0){
           return ServiceResponse.createByErrorMessage("旧密码错误");
       }

       user.setPassword(MD5Util.MD5EncodeUtf8(passwordNew));
       int updateCount = userMapper.updateByPrimaryKeySelective(user);
       if (updateCount > 0){
           return ServiceResponse.createBySuccessMessage("密码更新成功");
       }
       return ServiceResponse.createByErrorMessage("密码更新失败");

    }

    public ServiceResponse<User> updateInformation(User user){
        //username是不能被更新的
        //email也需要进行一个检验，检验新的email是否已经存在，并且存在的email与新的email相同的话，不能是当前这个用户的
        int resultCount = userMapper.checkEmailByUserId(user.getEmail(),user.getId());
        if (resultCount>0){
            return  ServiceResponse.createByErrorMessage("该邮箱已经存在，请更换email再尝试更新");
        }

        User updateUser = new User();
        updateUser.setId(user .getId());
        updateUser.setEmail(user.getEmail());
        updateUser.setAnswer(user.getAnswer());
        updateUser.setPhone(user.getPhone());
        updateUser.setQuestion(user.getQuestion());

        int updateCount = userMapper.updateByPrimaryKeySelective(updateUser);
        if (updateCount > 0){
            return  ServiceResponse.createBySuccess("更新个人信息成功",updateUser);
        }
        return  ServiceResponse.createByErrorMessage("更新个人信息失败");

    }

    public  ServiceResponse<User> getInformation(Integer userId){
        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null){
            return ServiceResponse.createByErrorMessage("找不到当前用户");
        }
        user.setPassword(StringUtils.EMPTY);
        return  ServiceResponse.createBySuccess(user);

    }




  //backend

    /**
     * 校验是否是管理员
     * @param user
     * @return
     */
  public ServiceResponse checkAdminRole(User user){
        if (user.getRole().intValue() == Const.Role.ROLE_ADMIN && user!=null){
            return ServiceResponse.createBySuccess();
        }
        return ServiceResponse.createByError();
  }




}
