import sys
import io

def init_env(import_path: str):
    try:
        sys.path.index(import_path)
    except ValueError:
        sys.path.insert(0,import_path)
    import you_get

def extract(import_path: str, url: str):
    init_env(import_path)
    import you_get
    byte_buf = io.BytesIO()
    text_wrapper = io.TextIOWrapper(byte_buf, encoding='utf-8')

    old_stdout = sys.stdout
    sys.stdout = text_wrapper
    
    sys.argv = ['you-get', '--json',url]
    
    try:
        you_get.main()
        text_wrapper.flush()
        output = byte_buf.getvalue().decode('utf-8')
        return output
    except SystemExit:
        text_wrapper.flush()
        return byte_buf.getvalue().decode('utf-8')
    finally:
        sys.stdout = old_stdout