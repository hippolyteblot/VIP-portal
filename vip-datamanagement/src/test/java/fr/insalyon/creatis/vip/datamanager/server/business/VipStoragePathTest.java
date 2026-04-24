package fr.insalyon.creatis.vip.datamanager.server.business;

import fr.insalyon.creatis.vip.datamanager.models.VipStoragePath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VipStoragePathTest {

    @Test
    public void testVipPathStorageNormalization() {
        // nothing to do
        assertVipPath("/vip/Home/dir", "/vip/Home/dir");
        // managed ..
        assertVipPath("/vip/Home/somewhere/../dir", "/vip/Home/dir");
        // handle several slashes and trailing slashes
        assertVipPath("/vip///Home/dir/", "/vip/Home/dir");

    }

    public void assertVipPath(String original, String expected) {
        Assertions.assertEquals(
                VipStoragePath.of(null, original).getVipPath(),
                expected
        );
    }

}
