from flask import Flask, request, jsonify, send_file
import os

app = Flask(__name__)

def delete_old_file(user_id):
    for filename in os.listdir('uploads'):
        if filename.startswith(user_id):
            os.remove(os.path.join('uploads', filename))

# 上传文件
@app.route('/upload', methods=['POST'])
def upload_file():
    uploaded_file = request.files['file']
    if uploaded_file.filename != '':
        user_id, random_chars_extension = uploaded_file.filename.rsplit('_', 1)
        random_chars, extension = random_chars_extension.rsplit('.', 1)
        delete_old_file(user_id)

        file_path = os.path.join('uploads', uploaded_file.filename)
        uploaded_file.save(file_path)
        download_url = f"http://ia.tinksp.cn:38031/download/{uploaded_file.filename}"
        return jsonify({"status": "success", "download_url": download_url})
    else:
        return jsonify({"status": "failure", "message ": "没有上传文件。"})

# GET请求下载文件
@app.route('/download/<filename>', methods=['GET'])
def download_file(filename):
    file_path = os.path.join('uploads', filename)
    return send_file(file_path, as_attachment=True)

if __name__ == '__main__':
    if not os.path.exists('uploads'):
        os.makedirs('uploads')
    app.run(debug=True, port=8888 , host='127.0.0.1')