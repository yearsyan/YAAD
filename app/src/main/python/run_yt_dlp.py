import sys
import io
import zipimport

def run(args):
    pyz_path = list(args)[0]
    sys.argv = list(args)
    sys.stdout = buffer = io.StringIO()
    sys.path.insert(0, pyz_path)

    try:
        importer = zipimport.zipimporter(pyz_path)
        importer.load_module("__main__")
        return buffer.getvalue()
    except Exception as e:
        return pyz_path + " error: " + str(e)
    except SystemExit as e:
        if e.code == 0:
            return buffer.getvalue()
        return "error exit"
