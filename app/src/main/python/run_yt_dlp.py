import sys
import io
import json

def get_medias(result):
    if type(result) == str:
        result = json.loads(result)
    if type(result) != dict or result['entries'] is None or type(result['entries']) != list or len(result['entries']) == 0:
        return None
    entry = result['entries'][0]
    if not type(entry['requested_formats']) == list:
        return None
    ret = []
    for format_item in entry['requested_formats']:
        ret.append({
            'headers': format_item['http_headers'],
            'url': format_item['url'],
            'ext': format_item['ext'],
            'format_id': format_item['format_id'],
            'format': format_item['format'],
        })
    return ret

def run(args):
    pyz_path = list(args)[0]
    sys.argv = list(args)
    sys.stdout = buffer = io.StringIO()
    sys.path.insert(0, pyz_path)

    resp = '{}'
    try:
        import yt_dlp
        yt_dlp.main()
        resp = buffer.getvalue()
        return json.dumps({
            'result': get_medias(resp),
            'code': 0,
            'msg': 'ok'
        })
    except Exception as e:
        return json.dumps({
            'result': None,
            'code': 1,
            'msg': str(e)
        })
    except SystemExit as e:
        resp = buffer.getvalue()
        if e.code == 0:
            try:
                return json.dumps({
                    'result': get_medias(resp),
                    'code': 0,
                    'msg': 'ok'
                })
            except json.JSONDecodeError as e:
                return json.dumps({
                    'result': None,
                    'code': 2,
                    'msg': str(e) + 'resp: ' + resp
                })
            except Exception as e:
                return json.dumps({
                    'result': None,
                    'code': 1,
                    'msg': str(e)
                })
        else:
            return json.dumps({
                'result': None,
                'code': 1,
                'msg': str(e)
            })