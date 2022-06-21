
rmdir /S /Q "../src/main/resources/static/web_uploader"
mkdir "../src/main/resources/static/web_uploader"
xcopy "./dist" "../src/main/resources/static/web_uploader" /y /f /e
