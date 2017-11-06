// StringTok.h: interface for the CStringTok class.
//
//////////////////////////////////////////////////////////////////////

#if !defined(AFX_STRINGTOK_H__AAF07013_EA6B_4293_9C4D_7B45154ED4FA__INCLUDED_)
#define AFX_STRINGTOK_H__AAF07013_EA6B_4293_9C4D_7B45154ED4FA__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

class CStringTok  
{
public:
    unsigned int numTokens();
    CStringTok();
    CStringTok(LPTSTR  str, LPTSTR  seps, bool inpl = false);
    virtual ~CStringTok();

    void init(LPTSTR  str, LPTSTR  seps, bool inpl = false);
    LPTSTR  GetAt(int i);
    inline LPTSTR  CStringTok::operator[](int i){return(GetAt(i));}
    inline LPTSTR GetFirst(LPTSTR  str, LPTSTR  seps, bool inpl = false) {init(str,seps,inpl);curelem=0;return GetNext();}
    inline LPTSTR GetFirst() {curelem=0;return GetNext();}
    inline LPTSTR GetNext() {return GetAt(curelem++);}

private:
    LPTSTR *indexes;
    void init();
    unsigned int num_tokens;
    unsigned int str_len;
    LPTSTR  seps;
    LPTSTR  str;
    int curelem;
    bool inplace;
    char empty[1];
};

#endif // !defined(AFX_STRINGTOK_H__AAF07013_EA6B_4293_9C4D_7B45154ED4FA__INCLUDED_)
