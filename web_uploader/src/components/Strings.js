export function notEmpty(str)
{
  if(typeof str === 'string' && str.length > 0)
    return str;
  else
    return null;
}
