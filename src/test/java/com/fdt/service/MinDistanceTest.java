package com.fdt.service;

import com.fdt.utils.AlgorithmUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class MinDistanceTest {

    @Test
    void testCompareTags(){
        List<String> tagList1 = Arrays.asList("Java", "男", "后端");
        List<String> tagList2 = Arrays.asList("Java","女","前端");
        List<String> tagList3 = Arrays.asList("Java","女","后端");
        List<String> tagList4 = Arrays.asList("男","Java","后端");
        System.out.println(AlgorithmUtils.minDistance(tagList1, tagList2));
        System.out.println(AlgorithmUtils.minDistance(tagList1, tagList3));
        System.out.println(AlgorithmUtils.minDistance(tagList1, tagList4));
    }
}
