/*
 * Copyright 2022 Krzysztof Slusarski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.ks.viewer.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.experimental.UtilityClass;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.utils.IOUtils;
import org.tukaani.xz.XZInputStream;

@UtilityClass
public class InputUtils {
    public static InputStream getInputStream(String filename, String filePath) throws IOException {
        InputStream inputStream = null;
        if (filename.endsWith(".7z")) {
            inputStream = get7ZipInputStream(filePath);
        } else if (filename.endsWith(".zip")) {
            inputStream = getZipInputStream(filePath);
        } else if (filename.endsWith(".xz")) {
            inputStream = getXZInputStream(filePath);
        } else if (filename.endsWith(".gz") || filename.endsWith(".gzip")) {
            inputStream = getGZipInputStream(filePath);
        } else {
            inputStream = new FileInputStream(filePath);
        }
        return inputStream;
    }

    private static InputStream getXZInputStream(String saveFileName) throws IOException {
        XZInputStream xzInputStream = new XZInputStream(new FileInputStream(saveFileName));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        IOUtils.copy(xzInputStream, byteArrayOutputStream);
        xzInputStream.close();
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }

    private static InputStream getGZipInputStream(String saveFileName) throws IOException {
        GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(saveFileName));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        IOUtils.copy(gzipInputStream, byteArrayOutputStream);
        gzipInputStream.close();
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }

    private static InputStream getZipInputStream(String saveFileName) throws IOException {
        ZipFile zipFile = new ZipFile(saveFileName);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        ZipEntry entry = entries.nextElement();

        while (entries.hasMoreElements() && entry.isDirectory()) {
            entry = entries.nextElement();
        }

        InputStream inputStream = null;
        if (entry != null && !entry.isDirectory()) {
            inputStream = zipFile.getInputStream(entry);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            IOUtils.copy(inputStream, byteArrayOutputStream);
            inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        }

        zipFile.close();
        return inputStream;
    }

    private static InputStream get7ZipInputStream(String saveFileName) throws IOException {
        SevenZFile sevenZFile = new SevenZFile(new File(saveFileName));
        SevenZArchiveEntry entry = sevenZFile.getNextEntry();

        InputStream inputStream = null;
        while (entry != null && entry.isDirectory()) {
            entry = sevenZFile.getNextEntry();
        }

        if (entry != null) {
            byte[] content = new byte[(int) entry.getSize()];
            sevenZFile.read(content, 0, content.length);
            sevenZFile.close();
            inputStream = new ByteArrayInputStream(content);
        }

        sevenZFile.close();
        return inputStream;
    }
}
