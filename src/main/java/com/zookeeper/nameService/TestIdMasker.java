package com.zookeeper.nameService;

/**
 * 测试主键生成器
 *
 * @author wangruli
 * @date 2017/10/10 9:44
 */
public class TestIdMasker {

    public static void main(String[] args) throws Exception {

        IdMaker idMaker = new IdMaker("192.168.0.33:2181", "/NameService/IdGen", "ID");
        idMaker.start();

        try {
            for (int i = 0; i < 10; i++) {
                String id = idMaker.generateId(RemoveMethodEnum.DELAY);
                System.out.println(id);
            }
        } finally {
            idMaker.stop();
        }
    }
}
