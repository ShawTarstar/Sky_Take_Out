package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    SetmealMapper setmealMapper;
    @Autowired
    SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 新增套餐
     * @param setmealDTO
     */
    @Transactional
    public void save(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        //在套餐表中插入套餐数据
        setmealMapper.insert(setmeal);
        //在套餐菜品关系表中插入多条数据
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(setmeal.getId());
        }
        setmealDishMapper.insertBatch(setmealDishes);
    }

    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }

    /**
     * 批量删除套餐
     * @param ids
     */
    public void deleteBatch(List<Long> ids) {
        //如果id中有套餐正在起售，则不能删除
        for (Long id : ids) {
            SetmealVO setmealVO = selectById(id);
            if (setmealVO.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        setmealMapper.deleteBatch(ids);
    }

    public SetmealVO selectById(Long id) {
        //查询套餐基本信息
        SetmealVO setmealVO = setmealMapper.selectById(id);
        //查询套餐中的菜品
        List<SetmealDish> setmealDishes = setmealDishMapper.getSetmealDishesBySetmealId(id);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    @Transactional
    public void update(SetmealDTO setmealDTO) {
        //修改套餐表基本信息
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.update(setmeal);
        //修改套餐菜品表（删除再插入）
        setmealDishMapper.deleteBySetmealId(setmealDTO.getId());
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(setmealDTO.getId());
        }
        setmealDishMapper.insertBatch(setmealDishes);
    }

    public void startOrStop(Integer status, Long id) {
        //检查套餐中是否有未起售菜品
        List<SetmealDish> setmealDishes = setmealDishMapper.getSetmealDishesBySetmealId(id);
        for (SetmealDish setmealDish : setmealDishes) {
            Long dishId= setmealDish.getDishId();
            Dish dish=dishMapper.getById(dishId);
            log.info("菜品：{}",dish);
            if (dish.getStatus() == StatusConstant.DISABLE){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ENABLE_FAILED);
            }
        }
         Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);

    }
}
