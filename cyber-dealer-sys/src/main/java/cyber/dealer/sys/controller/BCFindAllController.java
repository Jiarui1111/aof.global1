package cyber.dealer.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cyber.dealer.sys.constant.ReturnObject;
import cyber.dealer.sys.domain.CyberAgency;
import cyber.dealer.sys.domain.CyberUsers;
import cyber.dealer.sys.mapper.CyberAgencyMapper;
import cyber.dealer.sys.mapper.CyberUsersMapper;
import cyber.dealer.sys.util.ObjectToMapUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.web3j.crypto.Keys;
import springfox.documentation.annotations.ApiIgnore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cyber.dealer.sys.util.Common.decorateReturnObject;

/**
 * @author lfy
 * @Date 2022/5/5 10:39
 */
@RestController
@RequestMapping("BF")
@CrossOrigin
@Api(tags = "复杂查询的Controller")
@ApiIgnore
public class BCFindAllController {

    @Autowired
    private CyberUsersMapper cyberUsersMapper;

    @Autowired
    private CyberAgencyMapper cyberAgencyMapper;


    /**
     * 点上一级查询下一级的数据
     * @param address
     * @return
     */
    @GetMapping("findor")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "address", value = "钱包地址", required = true, dataType = "String", defaultValue = ""),
    })
    @ApiOperation(value ="点上一级查询下一级的数据的接口",notes = "")
    public Object findRegion(String address) {
        address = Keys.toChecksumAddress(address);
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("address", address);
        CyberUsers cyberUsers = cyberUsersMapper.selectOne(queryWrapper);

        QueryWrapper queryWrapper1 = new QueryWrapper();
        queryWrapper1.eq("uid", cyberUsers.getId());
        CyberAgency cyberAgency = cyberAgencyMapper.selectOne(queryWrapper1);
        if (cyberAgency == null) {
            return decorateReturnObject(new ReturnObject<>(false));
        }
        QueryWrapper<CyberUsers> queryWrapper2 = new QueryWrapper<>();
        queryWrapper2.eq("inv_id", cyberAgency.getId());
        queryWrapper2.orderByDesc("level");
        List<CyberUsers> list1 = cyberUsersMapper.selectList(queryWrapper2);

        List list = new ArrayList();

        if (!list1.isEmpty()) {
            for (CyberUsers cyberUserss : list1) {
                int level1 = 0;
                int level2 = 0;
                int level3 = 0;
                if (cyberUserss.getLevel() != 1) {
                    QueryWrapper<CyberAgency> queryWrapper4 = new QueryWrapper<>();
                    queryWrapper4.eq("uid", cyberUserss.getId());
                    CyberAgency cyberAgency4 = cyberAgencyMapper.selectOne(queryWrapper4);

                    QueryWrapper<CyberUsers> queryWrapper5 = new QueryWrapper<>();
                    queryWrapper5.eq("inv_id", cyberAgency4.getId());
                    List<CyberUsers> cyberUsers1 = cyberUsersMapper.selectList(queryWrapper5);
                    for (CyberUsers cyberUsersss : cyberUsers1) {
                        if (cyberUsersss.getLevel() == 1) {
                            level1 += 1;
                        }
                        if (cyberUsersss.getLevel() == 2) {
                            level2 += 1;
                        }
                        if (cyberUsersss.getLevel() == 3) {
                            level3 += 1;
                        }
                    }
                }

                Map convert = ObjectToMapUtil.convert(cyberUserss);
                convert.put("countlevel1", level1);
                convert.put("countlevel2", level2);
                convert.put("countlevel3", level3);
                list.add(convert);
            }
        }
        return decorateReturnObject(new ReturnObject<>(list));
    }

    /**
     * 分页查询 所有level  顶级inv_id为0的 根据创建时间进行排序
     * @param level
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("findalldatauser")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "level", value = "等级", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "page", value = "页数", required = true, dataType = "String", defaultValue = ""),
            @ApiImplicitParam(name = "pageSize", value = "数量", required = true, dataType = "String", defaultValue = ""),
    })
    @ApiOperation(value ="分页查询 所有level  顶级inv_id为0的 根据创建时间进行排序的接口",notes = "")
    public Object getFindAllDataUser(Integer level, Integer page, Integer pageSize) {
        QueryWrapper<CyberUsers> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("level", level);
        queryWrapper.eq("inv_id", 0);
        queryWrapper.orderByDesc("create_time");
        IPage<CyberUsers> pages = new Page<>(page, pageSize);
        IPage<CyberUsers> cyberUsersIPage = cyberUsersMapper.selectPage(pages, queryWrapper);
        List<CyberUsers> cyberUserss = cyberUsersIPage.getRecords();

        long pages1 = cyberUsersIPage.getPages();
        long total = cyberUsersIPage.getTotal();
        long current = cyberUsersIPage.getCurrent();

        List list = new ArrayList();


        if (!cyberUserss.isEmpty()) {
            for (CyberUsers cyberUsers : cyberUserss) {
                int level1 = 0;
                int level2 = 0;
                int level3 = 0;
                if (cyberUsers.getLevel() != 1) {
                    QueryWrapper<CyberAgency> queryWrapper1 = new QueryWrapper<>();
                    queryWrapper1.eq("uid", cyberUsers.getId());
                    CyberAgency cyberAgency = cyberAgencyMapper.selectOne(queryWrapper1);

                    QueryWrapper<CyberUsers> queryWrapper2 = new QueryWrapper<>();
                    queryWrapper2.eq("inv_id", cyberAgency.getId());
                    List<CyberUsers> cyberUsers1 = cyberUsersMapper.selectList(queryWrapper2);
                    for (CyberUsers cyberUsersss : cyberUsers1) {
                        if (cyberUsersss.getLevel() == 1) {
                            level1 += 1;
                        }
                        if (cyberUsersss.getLevel() == 2) {
                            level2 += 1;
                        }
                        if (cyberUsersss.getLevel() == 3) {
                            level3 += 1;
                        }
                    }
                }

                Map convert = ObjectToMapUtil.convert(cyberUsers);
                convert.put("countlevel1", level1);
                convert.put("countlevel2", level2);
                convert.put("countlevel3", level3);
                list.add(convert);
            }

            Map<String, Object> map = new HashMap<>();
            map.put("pages", pages1);
            map.put("total", total);
            map.put("current", current);

            list.add(map);
            return decorateReturnObject(new ReturnObject<>(list));
        }
        return decorateReturnObject(new ReturnObject<>(false));
    }


    /**
     * 查询所有的用户阶段数据 个数
     * @return
     */
    @GetMapping("findAlldata")
    @ApiImplicitParams({
    })
    @ApiOperation(value ="查询所有的用户阶段数据 个数的接口",notes = "")
    public Object findAlldata() {
        //国家级人数
        QueryWrapper queryWrapper1 = new QueryWrapper();
        queryWrapper1
                .eq("level", 4);
        Integer level4count = cyberUsersMapper.selectCount(queryWrapper1);
        //区域人数
        QueryWrapper queryWrapper2 = new QueryWrapper();
        queryWrapper2
                .eq("level", 3);
        Integer level3count = cyberUsersMapper.selectCount(queryWrapper2);
        //伙伴人数
        QueryWrapper queryWrapper3 = new QueryWrapper();
        queryWrapper3
                .eq("level", 2);
        Integer level2count = cyberUsersMapper.selectCount(queryWrapper3);
        //用户
        QueryWrapper queryWrapper4 = new QueryWrapper();
        queryWrapper4
                .eq("level", 1);
        Integer level1count = cyberUsersMapper.selectCount(queryWrapper4);

        Map map = new HashMap();
        map.put("level4count", level4count);
        map.put("level3count", level3count);
        map.put("level2count", level2count);
        map.put("level1count", level1count);

        return decorateReturnObject(new ReturnObject<>(map));
    }
}
